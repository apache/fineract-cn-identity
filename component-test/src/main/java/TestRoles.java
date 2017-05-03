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

import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.identity.api.v1.domain.Permission;
import io.mifos.identity.api.v1.domain.Role;
import io.mifos.identity.api.v1.events.EventConstants;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.mifos.identity.internal.util.IdentityConstants.SU_ROLE;

/**
 * @author Myrle Krantz
 */
public class TestRoles extends AbstractComponentTest {
  @Test
  public void testRolesSortedAlphabetically() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final Permission rolePermission = buildRolePermission();

      final Role role1 = buildRole(testEnvironment.generateUniqueIdentifer("abba"), rolePermission);
      final Role role2 = buildRole(testEnvironment.generateUniqueIdentifer("bubba"), rolePermission);
      final Role role3 = buildRole(testEnvironment.generateUniqueIdentifer("c1"), rolePermission);
      final Role role4 = buildRole(testEnvironment.generateUniqueIdentifer("calla"), rolePermission);
      final Role role5 = buildRole(testEnvironment.generateUniqueIdentifer("uelf"), rolePermission);
      final Role role6 = buildRole(testEnvironment.generateUniqueIdentifer("ulf"), rolePermission);

      getTestSubject().createRole(role2);
      getTestSubject().createRole(role1);
      getTestSubject().createRole(role6);
      getTestSubject().createRole(role4);
      getTestSubject().createRole(role3);
      getTestSubject().createRole(role5);

      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role1.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role2.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role3.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role4.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role5.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role6.getIdentifier()));

      final List<Role> roles = getTestSubject().getRoles();
      final List<String> idList = roles.stream().map(Role::getIdentifier).collect(Collectors.toList());
      final List<String> sortedList = Arrays.asList(
              role1.getIdentifier(),
              role2.getIdentifier(),
              role3.getIdentifier(),
              role4.getIdentifier(),
              role5.getIdentifier(),
              role6.getIdentifier());
      final List<String> filterOutIdsFromOtherTests = idList.stream().filter(sortedList::contains).collect(Collectors.toList());
      Assert.assertEquals(sortedList, filterOutIdsFromOtherTests);
    }
  }

  @Test
  public void testCreateRole() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String roleIdentifier = generateRoleIdentifier();

      final Permission rolePermission = buildRolePermission();
      final Role scribe = buildRole(roleIdentifier, rolePermission);

      getTestSubject().createRole(scribe);

      {
        final boolean found = eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, scribe.getIdentifier());
        Assert.assertTrue(found);
      }

      final List<Role> roles = getTestSubject().getRoles();
      Assert.assertTrue(Helpers.instancePresent(roles, Role::getIdentifier, roleIdentifier));

      final Role role = getTestSubject().getRole(roleIdentifier);
      Assert.assertNotNull(role);
      Assert.assertEquals(roleIdentifier, role.getIdentifier());
      Assert.assertEquals(Collections.singletonList(rolePermission), role.getPermissions());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotBeAbleToCreateRoleNamedDeactivated() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final Permission rolePermission = buildRolePermission();
      final Role deactivated = buildRole("deactivated", rolePermission);

      getTestSubject().createRole(deactivated);
    }
  }

  @Test
  public void deleteRole() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String roleIdentifier = createRoleManagementRole();

      final Role role = getTestSubject().getRole(roleIdentifier);
      Assert.assertNotNull(role);

      getTestSubject().deleteRole(role.getIdentifier());

      {
        final boolean found = eventRecorder.wait(EventConstants.OPERATION_DELETE_ROLE, roleIdentifier);
        Assert.assertTrue(found);
      }

      final List<Role> roles = getTestSubject().getRoles();
      Assert.assertFalse(Helpers.instancePresent(roles, Role::getIdentifier, roleIdentifier));
    }
  }

  @Test(expected= NotFoundException.class)
  public void deleteRoleThatDoesntExist() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String randomIdentifier = generateRoleIdentifier();

      getTestSubject().deleteRole(randomIdentifier);
    }
  }

  @Test()
  public void changeRole() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String roleIdentifier = createRoleManagementRole();

      final Role role = getTestSubject().getRole(roleIdentifier);
      role.getPermissions().add(buildUserPermission());

      getTestSubject().changeRole(roleIdentifier, role);

      {
        final boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_ROLE, role.getIdentifier());
        Assert.assertTrue(found);
      }

      final Role changedRole = getTestSubject().getRole(roleIdentifier);
      Assert.assertEquals(role, changedRole);
    }
  }

  @Test
  public void testChangePharaohRoleFails() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final Role referenceRole = getTestSubject().getRole(SU_ROLE);
      final Role roleChangeRequest = buildRole(SU_ROLE, buildSelfPermission());

      try {
        getTestSubject().changeRole(SU_ROLE, roleChangeRequest);
        Assert.fail("Should not be able to change the pharaoh role.");
      }
      catch (final IllegalArgumentException expected) {
        //noinspection EmptyCatchBlock
      }

      final Role unChangedRole = getTestSubject().getRole(SU_ROLE);
      Assert.assertEquals(referenceRole, unChangedRole);
    }
  }

  @Test
  public void testDeletePharaohRoleFails() throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {
      final Role adminRole = getTestSubject().getRole(ADMIN_ROLE);
      try {
        getTestSubject().deleteRole(ADMIN_ROLE);
        Assert.fail("It should not be possible to delete the admin role.");
      }
      catch (final IllegalArgumentException expected) {
        //noinspection EmptyCatchBlock
      }

      final Role adminRoleStillThere = getTestSubject().getRole(ADMIN_ROLE);
      Assert.assertEquals(adminRole, adminRoleStillThere);
    }
  }
}