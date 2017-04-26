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
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.core.lang.security.RsaKeyPairFactory;
import io.mifos.identity.api.v1.PermittableGroupIds;
import io.mifos.identity.api.v1.domain.Permission;
import io.mifos.identity.api.v1.events.ApplicationPermissionEvent;
import io.mifos.identity.api.v1.events.ApplicationPermissionUserEvent;
import io.mifos.identity.api.v1.events.ApplicationSignatureEvent;
import io.mifos.identity.api.v1.events.EventConstants;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestApplications extends AbstractComponentTest {

  @Test
  public void testSetApplicationSignature() throws InterruptedException {
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureEvent appPlusSig = setApplicationSignature();

      final List<String> foundApplications = getTestSubject().getApplications();
      Assert.assertTrue(foundApplications.contains(appPlusSig.getApplicationIdentifier()));

      getTestSubject().getApplicationSignature(
              appPlusSig.getApplicationIdentifier(),
              appPlusSig.getKeyTimestamp());
    }
  }

  @Test
  public void testCreateAndDeleteApplicationPermission() throws InterruptedException {
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureEvent appPlusSig = setApplicationSignature();

      final Permission identityManagementPermission = new Permission();
      identityManagementPermission.setPermittableEndpointGroupIdentifier(PermittableGroupIds.IDENTITY_MANAGEMENT);
      identityManagementPermission.setAllowedOperations(Collections.singleton(AllowedOperation.READ));

      getTestSubject().createApplicationPermission(appPlusSig.getApplicationIdentifier(), identityManagementPermission);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_PERMISSION,
              new ApplicationPermissionEvent(appPlusSig.getApplicationIdentifier(), PermittableGroupIds.IDENTITY_MANAGEMENT)));

      {
        final List<Permission> applicationPermissions = getTestSubject().getApplicationPermissions(appPlusSig.getApplicationIdentifier());
        Assert.assertTrue(applicationPermissions.contains(identityManagementPermission));
      }

      final Permission roleManagementPermission = new Permission();
      roleManagementPermission.setPermittableEndpointGroupIdentifier(PermittableGroupIds.ROLE_MANAGEMENT);
      roleManagementPermission.setAllowedOperations(Collections.singleton(AllowedOperation.READ));

      getTestSubject().createApplicationPermission(appPlusSig.getApplicationIdentifier(), roleManagementPermission);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_PERMISSION,
              new ApplicationPermissionEvent(appPlusSig.getApplicationIdentifier(), PermittableGroupIds.ROLE_MANAGEMENT)));
      {
        final List<Permission> applicationPermissions = getTestSubject().getApplicationPermissions(appPlusSig.getApplicationIdentifier());
        Assert.assertTrue(applicationPermissions.contains(identityManagementPermission));
        Assert.assertTrue(applicationPermissions.contains(roleManagementPermission));
      }

      getTestSubject().deleteApplicationPermission(appPlusSig.getApplicationIdentifier(), identityManagementPermission.getPermittableEndpointGroupIdentifier());
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_DELETE_APPLICATION_PERMISSION,
              new ApplicationPermissionEvent(appPlusSig.getApplicationIdentifier(), PermittableGroupIds.IDENTITY_MANAGEMENT)));

      {
        final List<Permission> applicationPermissions = getTestSubject().getApplicationPermissions(appPlusSig.getApplicationIdentifier());
        Assert.assertFalse(applicationPermissions.contains(identityManagementPermission));
        Assert.assertTrue(applicationPermissions.contains(roleManagementPermission));
      }
    }
  }

  @Test
  public void testDeleteApplication() throws InterruptedException {
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureEvent appPlusSig = setApplicationSignature();

      getTestSubject().deleteApplication(appPlusSig.getApplicationIdentifier());

      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_DELETE_APPLICATION, appPlusSig.getApplicationIdentifier()));

      final List<String> foundApplications = getTestSubject().getApplications();
      Assert.assertFalse(foundApplications.contains(appPlusSig.getApplicationIdentifier()));

      try {
        getTestSubject().getApplicationSignature(
                appPlusSig.getApplicationIdentifier(),
                appPlusSig.getKeyTimestamp());
        Assert.fail("Shouldn't find app sig after app was deleted.");
      }
      catch (final NotFoundException ignored2) {

      }
    }
  }

  @Test
  public void testApplicationPermissionUserApprovalProvisioning() throws InterruptedException {
    final ApplicationSignatureEvent appPlusSig;
    final Permission identityManagementPermission;
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      appPlusSig = setApplicationSignature();

      identityManagementPermission = new Permission(
              PermittableGroupIds.ROLE_MANAGEMENT,
              Collections.singleton(AllowedOperation.READ));

      getTestSubject().createApplicationPermission(appPlusSig.getApplicationIdentifier(), identityManagementPermission);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_PERMISSION,
              new ApplicationPermissionEvent(appPlusSig.getApplicationIdentifier(),
                      identityManagementPermission.getPermittableEndpointGroupIdentifier())));
    }

    final String user1Password;
    final String user1id;
    final String user2Password;
    final String user2id;
    try (final AutoUserContext ignored = enableAndLoginAdmin()) {
      final String selfManagementRoleId = createSelfManagementRole();
      final String roleManagementRoleId = createRoleManagementRole();

      user1Password = RandomStringUtils.randomAlphanumeric(5);
      user1id = createUserWithNonexpiredPassword(user1Password, selfManagementRoleId);

      user2Password = RandomStringUtils.randomAlphanumeric(5);
      user2id = createUserWithNonexpiredPassword(user2Password, roleManagementRoleId);
    }

    try (final AutoUserContext ignored = loginUser(user1id, user1Password)) {
      Assert.assertFalse(getTestSubject().getApplicationPermissionEnabledForUser(
              appPlusSig.getApplicationIdentifier(),
              identityManagementPermission.getPermittableEndpointGroupIdentifier(),
              user1id));

      getTestSubject().setApplicationPermissionEnabledForUser(
              appPlusSig.getApplicationIdentifier(),
              identityManagementPermission.getPermittableEndpointGroupIdentifier(),
              user1id,
              true);

      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_PERMISSION_USER_ENABLED,
              new ApplicationPermissionUserEvent(
                      appPlusSig.getApplicationIdentifier(),
                      identityManagementPermission.getPermittableEndpointGroupIdentifier(),
                      user1id)));

      Assert.assertTrue(getTestSubject().getApplicationPermissionEnabledForUser(
              appPlusSig.getApplicationIdentifier(),
              identityManagementPermission.getPermittableEndpointGroupIdentifier(),
              user1id));
    }

    try (final AutoUserContext ignored = loginUser(user2id, user2Password)) {
      Assert.assertFalse(getTestSubject().getApplicationPermissionEnabledForUser(
              appPlusSig.getApplicationIdentifier(),
              identityManagementPermission.getPermittableEndpointGroupIdentifier(),
              user2id));
    }

    try (final AutoUserContext ignored = loginUser(user1id, user1Password)) {
      getTestSubject().setApplicationPermissionEnabledForUser(
              appPlusSig.getApplicationIdentifier(),
              identityManagementPermission.getPermittableEndpointGroupIdentifier(),
              user1id,
              false);

      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_PERMISSION_USER_ENABLED,
              new ApplicationPermissionUserEvent(
                      appPlusSig.getApplicationIdentifier(),
                      identityManagementPermission.getPermittableEndpointGroupIdentifier(),
                      user1id)));
    }

    //Note that at this point, our imaginary application still cannot do anything in the name of any user,
    //because neither of the users has the permission the user enabled for the application.
  }

  @Test
  public void applicationIssuedRefreshTokenHappyCase() throws InterruptedException {
    final ApplicationSignatureEvent appPlusSig;
    final Permission rolePermission = buildRolePermission();
    final Permission userPermission = buildUserPermission();
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      appPlusSig = setApplicationSignature();

      getTestSubject().createApplicationPermission(appPlusSig.getApplicationIdentifier(), rolePermission);
      getTestSubject().createApplicationPermission(appPlusSig.getApplicationIdentifier(), userPermission);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_PERMISSION,
              new ApplicationPermissionEvent(appPlusSig.getApplicationIdentifier(),
                      rolePermission.getPermittableEndpointGroupIdentifier())));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_PERMISSION,
              new ApplicationPermissionEvent(appPlusSig.getApplicationIdentifier(),
                      userPermission.getPermittableEndpointGroupIdentifier())));
    }

    final String userid;
    final String userPassword;
    try (final AutoUserContext ignored = enableAndLoginAdmin()) {
      final String selfManagementRoleId = createRole(rolePermission, userPermission);

      userPassword = RandomStringUtils.randomAlphanumeric(5);
      userid = createUserWithNonexpiredPassword(userPassword, selfManagementRoleId);
    }


    try (final AutoUserContext ignored = loginUser(userid, userPassword)) {
      getTestSubject().setApplicationPermissionEnabledForUser(
              appPlusSig.getApplicationIdentifier(),
              userPermission.getPermittableEndpointGroupIdentifier(),
              userid,
              true);
    }
    //TODO: get me a refresh token here. use it to get an access token.  Then access like mad.
  }

  private String createTestApplicationName()
  {
    return "test" + RandomStringUtils.randomNumeric(3) + "-v1";
  }

  private ApplicationSignatureEvent setApplicationSignature() throws InterruptedException {
    final String testApplicationName = createTestApplicationName();
    final RsaKeyPairFactory.KeyPairHolder keyPair = RsaKeyPairFactory.createKeyPair();
    final Signature signature = new Signature(keyPair.getPublicKeyMod(), keyPair.getPublicKeyExp());

    getTestSubject().setApplicationSignature(testApplicationName, keyPair.getTimestamp(), signature);

    final ApplicationSignatureEvent event = new ApplicationSignatureEvent(testApplicationName, keyPair.getTimestamp());
    Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE, event));
    return event;
  }
}
