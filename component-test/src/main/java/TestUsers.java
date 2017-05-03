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

import io.mifos.anubis.api.v1.domain.AllowedOperation;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.test.env.TestEnvironment;
import io.mifos.identity.api.v1.PermittableGroupIds;
import io.mifos.identity.api.v1.domain.*;
import io.mifos.identity.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.mifos.identity.internal.util.IdentityConstants.SU_NAME;
import static io.mifos.identity.internal.util.IdentityConstants.SU_ROLE;

/**
 * @author Myrle Krantz
 */
public class TestUsers extends AbstractComponentTest {


  @Test
  public void testAddLogin() throws InterruptedException {

    final String username = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    try (final AutoUserContext ignore = loginAdmin()) {
      final User user = getTestSubject().getUser(username);
      Assert.assertNotNull(user);
      Assert.assertEquals("Correct user identifier?", username, user.getIdentifier());
      Assert.assertEquals("Correct role?", ADMIN_ROLE, user.getRole());
    }

    final Authentication userAuthentication =
            getTestSubject().login(username, TestEnvironment.encodePassword(AHMES_PASSWORD));

    Assert.assertNotNull(userAuthentication);

    try (final AutoUserContext ignored = new AutoUserContext(username, userAuthentication.getAccessToken())) {
      getTestSubject().createUser(new UserWithPassword("Ahmes_friend", "scribe",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD)));

      final boolean found = eventRecorder.wait(EventConstants.OPERATION_POST_USER, "Ahmes_friend");
      Assert.assertTrue(found);
    }

    try (final AutoUserContext ignore = loginAdmin()) {
      final List<User> users = getTestSubject().getUsers();
      Assert.assertTrue(Helpers.instancePresent(users, User::getIdentifier, username));
      Assert.assertTrue(Helpers.instancePresent(users, User::getIdentifier, "Ahmes_friend"));
    }
  }

  @Test
  public void testChangeUserRole() throws InterruptedException {
    final String userIdentifier = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication ahmesAuthentication =
            getTestSubject().login(userIdentifier, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(userIdentifier, ahmesAuthentication.getAccessToken())) {
      List<User> users = getTestSubject().getUsers();
      Assert.assertEquals(2, users.size());

      getTestSubject().changeUserRole(userIdentifier, new RoleIdentifier("scribe"));

      final boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_USER_ROLEIDENTIFIER, userIdentifier);
      Assert.assertTrue(found);

      final User ahmes = getTestSubject().getUser(userIdentifier);
      Assert.assertEquals("scribe", ahmes.getRole());

      final Set<Permission> userPermittableGroups = getTestSubject().getUserPermissions(userIdentifier);
      Assert.assertTrue(userPermittableGroups.contains(new Permission(PermittableGroupIds.SELF_MANAGEMENT, AllowedOperation.ALL)));

      users = getTestSubject().getUsers();
      Assert.assertEquals(2, users.size());
    }
  }

  @Test
  public void testChangeAntonyRoleFails() throws InterruptedException {
    final String userIdentifier = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication ahmesAuthentication =
            getTestSubject().login(userIdentifier, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(userIdentifier, ahmesAuthentication.getAccessToken())) {
      try {
        getTestSubject().changeUserRole(SU_NAME, new RoleIdentifier("scribe"));
        Assert.fail("Should not be able to change the role set for antony.");
      }
      catch (final IllegalArgumentException expected) {
        //noinspection EmptyCatchBlock
      }

      final User antony = getTestSubject().getUser(SU_NAME);
      Assert.assertEquals(SU_ROLE, antony.getRole());
    }
  }

  @Test
  public void testAdminProvisioning() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final List<Role> roleIdentifiers = getTestSubject().getRoles();
      Assert.assertTrue(Helpers.instancePresent(roleIdentifiers, Role::getIdentifier, ADMIN_ROLE));

      final Role role = getTestSubject().getRole(ADMIN_ROLE);
      Assert.assertNotNull(role);
      Assert.assertTrue(role.getPermissions().contains(constructFullAccessPermission(PermittableGroupIds.IDENTITY_MANAGEMENT)));
      Assert.assertTrue(role.getPermissions().contains(constructFullAccessPermission(PermittableGroupIds.ROLE_MANAGEMENT)));

      final List<User> userIdentifiers = getTestSubject().getUsers();
      Assert.assertTrue(Helpers.instancePresent(userIdentifiers, User::getIdentifier, ADMIN_IDENTIFIER));

      final User user = getTestSubject().getUser(ADMIN_IDENTIFIER);
      Assert.assertNotNull(user);
      Assert.assertEquals(ADMIN_IDENTIFIER, user.getIdentifier());
      Assert.assertEquals(ADMIN_ROLE, user.getRole());

      final Set<Permission> adminPermittableGroups = getTestSubject().getUserPermissions(ADMIN_IDENTIFIER);
      Assert.assertTrue(adminPermittableGroups.contains(new Permission(PermittableGroupIds.SELF_MANAGEMENT, AllowedOperation.ALL)));
      Assert.assertTrue(adminPermittableGroups.contains(new Permission(PermittableGroupIds.IDENTITY_MANAGEMENT, AllowedOperation.ALL)));
      Assert.assertTrue(adminPermittableGroups.contains(new Permission(PermittableGroupIds.ROLE_MANAGEMENT, AllowedOperation.ALL)));
    }
  }

  private Permission constructFullAccessPermission(final String permittableGroupId) {
    final HashSet<AllowedOperation> allowedOperations = new HashSet<>();
    allowedOperations.add(AllowedOperation.CHANGE);
    allowedOperations.add(AllowedOperation.DELETE);
    allowedOperations.add(AllowedOperation.READ);
    return new Permission(permittableGroupId, allowedOperations);
  }
}
