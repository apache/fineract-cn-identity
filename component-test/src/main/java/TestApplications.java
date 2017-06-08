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
import io.mifos.anubis.token.TenantRefreshTokenSerializer;
import io.mifos.anubis.token.TokenSerializationResult;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.identity.api.v1.PermittableGroupIds;
import io.mifos.identity.api.v1.domain.Authentication;
import io.mifos.identity.api.v1.domain.CallEndpointSet;
import io.mifos.identity.api.v1.domain.Permission;
import io.mifos.identity.api.v1.domain.User;
import io.mifos.identity.api.v1.events.ApplicationCallEndpointSetEvent;
import io.mifos.identity.api.v1.events.ApplicationPermissionEvent;
import io.mifos.identity.api.v1.events.ApplicationPermissionUserEvent;
import io.mifos.identity.api.v1.events.EventConstants;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestApplications extends AbstractComponentTest {

  private static final String CALL_ENDPOINT_SET_IDENTIFIER = "doughboy";

  @Test
  public void testSetApplicationSignature() throws InterruptedException {
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData appPlusSig = setApplicationSignature();

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
      final ApplicationSignatureTestData appPlusSig = setApplicationSignature();

      final Permission identityManagementPermission = new Permission();
      identityManagementPermission.setPermittableEndpointGroupIdentifier(PermittableGroupIds.IDENTITY_MANAGEMENT);
      identityManagementPermission.setAllowedOperations(Collections.singleton(AllowedOperation.READ));

      createApplicationPermission(appPlusSig.getApplicationIdentifier(), identityManagementPermission);

      {
        final List<Permission> applicationPermissions = getTestSubject().getApplicationPermissions(appPlusSig.getApplicationIdentifier());
        Assert.assertTrue(applicationPermissions.contains(identityManagementPermission));

        final Permission applicationPermission = getTestSubject().getApplicationPermission(appPlusSig.getApplicationIdentifier(), PermittableGroupIds.IDENTITY_MANAGEMENT);
        Assert.assertEquals(identityManagementPermission, applicationPermission);
      }

      final Permission roleManagementPermission = new Permission();
      roleManagementPermission.setPermittableEndpointGroupIdentifier(PermittableGroupIds.ROLE_MANAGEMENT);
      roleManagementPermission.setAllowedOperations(Collections.singleton(AllowedOperation.READ));

      createApplicationPermission(appPlusSig.getApplicationIdentifier(), roleManagementPermission);

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
      final ApplicationSignatureTestData appPlusSig = setApplicationSignature();

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
    final ApplicationSignatureTestData appPlusSig;
    final Permission identityManagementPermission;
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      appPlusSig = setApplicationSignature();

      identityManagementPermission = new Permission(
              PermittableGroupIds.ROLE_MANAGEMENT,
              Collections.singleton(AllowedOperation.READ));

      createApplicationPermission(appPlusSig.getApplicationIdentifier(), identityManagementPermission);
    }

    final String user1Password;
    final String user1id;
    final String user2Password;
    final String user2id;
    try (final AutoUserContext ignored = loginAdmin()) {
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
  public void manageApplicationEndpointSet() throws InterruptedException {
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData appPlusSig = setApplicationSignature();

      final String endpointSetIdentifier = testEnvironment.generateUniqueIdentifer("epset");
      final CallEndpointSet endpointSet = new CallEndpointSet();
      endpointSet.setIdentifier(endpointSetIdentifier);
      endpointSet.setPermittableEndpointGroupIdentifiers(Collections.emptyList());

      getTestSubject().createApplicationCallEndpointSet(appPlusSig.getApplicationIdentifier(), endpointSet);

      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(appPlusSig.getApplicationIdentifier(), endpointSetIdentifier)));

      final List<CallEndpointSet> applicationEndpointSets = getTestSubject().getApplicationCallEndpointSets(appPlusSig.getApplicationIdentifier());
      Assert.assertTrue(applicationEndpointSets.contains(endpointSet));

      final CallEndpointSet storedEndpointSet = getTestSubject().getApplicationCallEndpointSet(
              appPlusSig.getApplicationIdentifier(),
              endpointSetIdentifier);
      Assert.assertEquals(endpointSet, storedEndpointSet);

      endpointSet.setPermittableEndpointGroupIdentifiers(Collections.singletonList(PermittableGroupIds.ROLE_MANAGEMENT));
      getTestSubject().changeApplicationCallEndpointSet(
              appPlusSig.getApplicationIdentifier(),
              endpointSetIdentifier,
              endpointSet);

      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(appPlusSig.getApplicationIdentifier(), endpointSetIdentifier)));

      final CallEndpointSet storedEndpointSet2 = getTestSubject().getApplicationCallEndpointSet(
              appPlusSig.getApplicationIdentifier(),
              endpointSetIdentifier);
      Assert.assertEquals(endpointSet, storedEndpointSet2);

      final List<CallEndpointSet> applicationEndpointSets2 = getTestSubject().getApplicationCallEndpointSets(appPlusSig.getApplicationIdentifier());
      Assert.assertTrue(applicationEndpointSets2.size() == 1);

      getTestSubject().deleteApplicationCallEndpointSet(appPlusSig.getApplicationIdentifier(), endpointSetIdentifier);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_DELETE_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(appPlusSig.getApplicationIdentifier(), endpointSetIdentifier)));

      final List<CallEndpointSet> applicationEndpointSets3 = getTestSubject().getApplicationCallEndpointSets(appPlusSig.getApplicationIdentifier());
      Assert.assertTrue(applicationEndpointSets3.isEmpty());
    }
  }

  @Test
  public void applicationIssuedRefreshTokenHappyCase() throws InterruptedException {
    final ApplicationSignatureTestData appPlusSig;
    final Permission rolePermission = buildRolePermission();
    final Permission userPermission = buildUserPermission();
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      appPlusSig = setApplicationSignature();

      createApplicationPermission(appPlusSig.getApplicationIdentifier(), rolePermission);
      createApplicationPermission(appPlusSig.getApplicationIdentifier(), userPermission);

      getTestSubject().createApplicationCallEndpointSet(
              appPlusSig.getApplicationIdentifier(),
              new CallEndpointSet(CALL_ENDPOINT_SET_IDENTIFIER,
                      Arrays.asList(rolePermission.getPermittableEndpointGroupIdentifier(),
                              userPermission.getPermittableEndpointGroupIdentifier())));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(appPlusSig.getApplicationIdentifier(),
                      CALL_ENDPOINT_SET_IDENTIFIER)));
    }

    final String userid;
    final String userPassword;
    try (final AutoUserContext ignored = loginAdmin()) {
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
      getTestSubject().setApplicationPermissionEnabledForUser(
              appPlusSig.getApplicationIdentifier(),
              rolePermission.getPermittableEndpointGroupIdentifier(),
              userid,
              true);
    }

    final TokenSerializationResult tokenSerializationResult =
            new TenantRefreshTokenSerializer().build(new TenantRefreshTokenSerializer.Specification()
                    .setUser(userid)
                    .setEndpointSet(CALL_ENDPOINT_SET_IDENTIFIER)
                    .setSecondsToLive(30)
                    .setKeyTimestamp(appPlusSig.getKeyTimestamp())
                    .setPrivateKey(appPlusSig.getKeyPair().privateKey())
                    .setSourceApplication(appPlusSig.getApplicationIdentifier()));


    final Authentication applicationAuthentication = getTestSubject().refresh(tokenSerializationResult.getToken());

    try (final AutoUserContext ignored = new AutoUserContext(userid, applicationAuthentication.getAccessToken())) {
      final List<User> users = getTestSubject().getUsers();
      Assert.assertFalse(users.isEmpty());
    }
  }

  @Test
  public void applicationIssuedRefreshTokenToCreatePermissionRequest() throws InterruptedException {
    final ApplicationSignatureTestData appPlusSig;
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      appPlusSig = setApplicationSignature();
      createApplicationPermission(appPlusSig.getApplicationIdentifier(), buildApplicationSelfPermission());
    }

    final String userid;
    final String userid2;
    final String userPassword;
    try (final AutoUserContext ignored = loginAdmin()) {

      final String roleId = createApplicationSelfManagementRole();

      userPassword = RandomStringUtils.randomAlphanumeric(5);
      userid = createUserWithNonexpiredPassword(userPassword, roleId);
      userid2 = createUserWithNonexpiredPassword(userPassword, roleId);

    }

    try (final AutoUserContext ignored = loginUser(userid, userPassword)) {
      getTestSubject().setApplicationPermissionEnabledForUser(appPlusSig.getApplicationIdentifier(), PermittableGroupIds.APPLICATION_SELF_MANAGEMENT, userid, true);
    }


    final TokenSerializationResult tokenSerializationResult =
            new TenantRefreshTokenSerializer().build(new TenantRefreshTokenSerializer.Specification()
                    .setUser(userid)
                    .setSecondsToLive(30)
                    .setKeyTimestamp(appPlusSig.getKeyTimestamp())
                    .setPrivateKey(appPlusSig.getKeyPair().privateKey())
                    .setSourceApplication(appPlusSig.getApplicationIdentifier()));


    final Authentication applicationAuthentication = getTestSubject().refresh(tokenSerializationResult.getToken());

    try (final AutoUserContext ignored = new AutoUserContext(userid, applicationAuthentication.getAccessToken())) {
      final Permission rolePermission = buildRolePermission();
      createApplicationPermission(appPlusSig.getApplicationIdentifier(), rolePermission);

      final List<Permission> appPermissions = getTestSubject().getApplicationPermissions(
              appPlusSig.getApplicationIdentifier());

      Assert.assertTrue(appPermissions.contains(rolePermission));

      try {
        getTestSubject().setApplicationPermissionEnabledForUser(appPlusSig.getApplicationIdentifier(), rolePermission.getPermittableEndpointGroupIdentifier(), userid2, true);
        Assert.fail("This call to create enable permission for another user should've failed.");
      }
      catch (final NotFoundException ignored2) {

      }

      try {
        createApplicationPermission("madeupname-v1", rolePermission);
        Assert.fail("This call to create application permission should've failed.");
      }
      catch (final NotFoundException ignored2) {

      }
    }
  }
}
