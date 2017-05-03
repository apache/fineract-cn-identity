/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.mifos.anubis.api.v1.client.Anubis;
import io.mifos.anubis.api.v1.domain.*;
import io.mifos.anubis.test.v1.SystemSecurityEnvironment;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.InvalidTokenException;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.core.lang.AutoTenantContext;
import io.mifos.core.lang.security.RsaPublicKeyBuilder;
import io.mifos.core.test.env.TestEnvironment;
import io.mifos.identity.api.v1.domain.*;
import org.junit.Assert;
import org.junit.Test;

import java.security.PublicKey;
import java.util.*;

import static io.mifos.identity.api.v1.events.EventConstants.*;

/**
 * @author Myrle Krantz
 */
public class TestAuthentication extends AbstractComponentTest {
  @Test
  //@Repeat(25)
  public void testAdminLogin() throws InterruptedException {
    //noinspection EmptyTryBlock
    try (final AutoUserContext ignore = loginAdmin()) {
    }
  }

  @Test
  public void testAdminLogout() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      getTestSubject().logout();

      try {
        getTestSubject().refresh();
        Assert.fail("Refresh should fail after logout has occurred.");
      }
      catch (final InvalidTokenException ignored)
      {
        //Expected.
      }
    }
  }

  @Test(expected = NotFoundException.class)
  public void testAdminIncorrectLogin() throws InterruptedException {
    getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword("set"));
    Assert.fail("login with wrong password should fail with not found exception.");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAdminMissingTenantHeader() throws InterruptedException {
    try (final AutoUserContext ignored = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      try (final AutoTenantContext ignored2 = new AutoTenantContext())
      {
        getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(ADMIN_PASSWORD));
      }
    }
    Assert.fail("login without tenant header set should fail with bad request.");
  }

  @Test()
  public void testPermissionsCorrectInAdminToken() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final Authentication adminAuthentication =
              getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(ADMIN_PASSWORD));
      Assert.assertNotNull(adminAuthentication);

      final TokenContent tokenContent = SystemSecurityEnvironment.getTokenContent(adminAuthentication.getAccessToken(), getPublicKey());
      final Set<TokenPermission> tokenPermissions = new HashSet<>(tokenContent.getTokenPermissions());

      final Set<TokenPermission> expectedTokenPermissions = new HashSet<>();
      Collections.addAll(expectedTokenPermissions,
              new TokenPermission("identity-v1/permittablegroups/*", Collections.singleton(AllowedOperation.CHANGE)),
              new TokenPermission("identity-v1/roles/*", Collections.singleton(AllowedOperation.DELETE)),
              new TokenPermission("identity-v1/users/*", Collections.singleton(AllowedOperation.READ)));
      //This is not a complete list.  This is a spot check.

      Assert.assertTrue("Expected: " + expectedTokenPermissions + "\nActual: " + tokenPermissions,
              tokenPermissions.containsAll(expectedTokenPermissions));
    }
  }

  @Test()
  public void testPermissionsCorrectInTokenWhenMultiplePermittableGroupsInRole() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final PermittableEndpoint horusEndpoint = buildPermittableEndpoint("horus");
      final PermittableGroup horusGroup = buildPermittableGroup("horus_Group", horusEndpoint);
      getTestSubject().createPermittableGroup(horusGroup);

      final PermittableEndpoint maatEndpoint = buildPermittableEndpoint("maat");
      final PermittableGroup maatGroup = buildPermittableGroup("maat_Group", maatEndpoint);
      getTestSubject().createPermittableGroup(maatGroup);

      Assert.assertTrue(eventRecorder.wait(OPERATION_POST_PERMITTABLE_GROUP, horusGroup.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(OPERATION_POST_PERMITTABLE_GROUP, maatGroup.getIdentifier()));

      final Permission horusGroupPermission = new Permission(horusGroup.getIdentifier(), Collections.singleton(AllowedOperation.READ));
      final Permission maatGroupPermission = new Permission(maatGroup.getIdentifier(), AllowedOperation.ALL);
      final Role compositeRole = new Role("composite_role", Arrays.asList(horusGroupPermission, maatGroupPermission));
      getTestSubject().createRole(compositeRole);

      Assert.assertTrue(eventRecorder.wait(OPERATION_POST_ROLE, compositeRole.getIdentifier()));

      final UserWithPassword user = new UserWithPassword("user_with_composite_role", compositeRole.getIdentifier(), "asdfasdfasdf");
      getTestSubject().createUser(user);

      Assert.assertTrue(eventRecorder.wait(OPERATION_POST_USER, user.getIdentifier()));

      final Authentication passwordChangeOnlyAuthentication = getTestSubject().login(user.getIdentifier(), user.getPassword());
      try (final AutoUserContext ignore2 = new AutoUserContext(user.getIdentifier(), passwordChangeOnlyAuthentication.getAccessToken()))
      {
        getTestSubject().changeUserPassword(user.getIdentifier(), new Password(user.getPassword()));

        Assert.assertTrue(eventRecorder.wait(OPERATION_PUT_USER_PASSWORD, user.getIdentifier()));
      }

      final Authentication authentication = getTestSubject().login(user.getIdentifier(), user.getPassword());
      final TokenContent tokenContent = SystemSecurityEnvironment.getTokenContent(authentication.getAccessToken(), getPublicKey());
      final Set<TokenPermission> tokenPermissions = new HashSet<>(tokenContent.getTokenPermissions());

      final Set<TokenPermission> expectedTokenPermissions= new HashSet<>();
      Collections.addAll(expectedTokenPermissions,
              new TokenPermission(horusEndpoint.getPath(), Collections.singleton(AllowedOperation.READ)),
              new TokenPermission(maatEndpoint.getPath(), Collections.singleton(AllowedOperation.READ)),
              new TokenPermission("identity-v1/users/{useridentifier}/password", Collections.singleton(AllowedOperation.CHANGE)),
              new TokenPermission("identity-v1/users/{useridentifier}/permissions", Collections.singleton(AllowedOperation.READ)),
              new TokenPermission("identity-v1/token/_current", Collections.singleton(AllowedOperation.DELETE)));

      Assert.assertTrue("Expected: " + expectedTokenPermissions + "\nActual: " + tokenPermissions,
              tokenPermissions.containsAll(expectedTokenPermissions));
    }
  }

  private PermittableGroup buildPermittableGroup(final String identifier, final PermittableEndpoint... permittableEndpoint) {
    final PermittableGroup ret = new PermittableGroup();
    ret.setIdentifier(identifier);
    ret.setPermittables(Arrays.asList(permittableEndpoint));
    return ret;
  }

  private PermittableEndpoint buildPermittableEndpoint(final String group) {
    final PermittableEndpoint ret = new PermittableEndpoint();
    ret.setPath(group + "/v1/x/y/z");
    ret.setMethod("GET");
    ret.setGroupId(group);
    return ret;
  }

  public PublicKey getPublicKey() {
    try (final AutoUserContext ignored = tenantApplicationSecurityEnvironment.createAutoSeshatContext())
    {
      final Anubis anubis = tenantApplicationSecurityEnvironment.getAnubis();
      final List<String> signatureKeyTimestamps = anubis.getAllSignatureSets();
      Assert.assertTrue(!signatureKeyTimestamps.isEmpty());
      final Signature sig = anubis.getApplicationSignature(signatureKeyTimestamps.get(0));

      return new RsaPublicKeyBuilder()
              .setPublicKeyMod(sig.getPublicKeyMod())
              .setPublicKeyExp(sig.getPublicKeyExp())
              .build();
    }
  }
}
