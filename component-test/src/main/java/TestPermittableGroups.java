/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.apache.fineract.cn.identity.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.identity.api.v1.domain.PermittableGroup;
import org.apache.fineract.cn.identity.api.v1.events.EventConstants;
import java.util.Collections;
import java.util.List;
import org.apache.fineract.cn.anubis.api.v1.domain.PermittableEndpoint;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Myrle Krantz
 */
public class TestPermittableGroups extends AbstractComponentTest {
  @Test
  public void getPermittableGroups() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final PermittableGroup identityManagementPermittableGroup
              = getTestSubject().getPermittableGroup(PermittableGroupIds.IDENTITY_MANAGEMENT);
      Assert.assertNotNull(identityManagementPermittableGroup);
      Assert.assertEquals(PermittableGroupIds.IDENTITY_MANAGEMENT, identityManagementPermittableGroup.getIdentifier());

      final PermittableGroup roleManagementPermittableGroup
              = getTestSubject().getPermittableGroup(PermittableGroupIds.ROLE_MANAGEMENT);
      Assert.assertNotNull(roleManagementPermittableGroup);
      Assert.assertEquals(PermittableGroupIds.ROLE_MANAGEMENT, roleManagementPermittableGroup.getIdentifier());

      final PermittableGroup selfManagementPermittableGroup
              = getTestSubject().getPermittableGroup(PermittableGroupIds.SELF_MANAGEMENT);
      Assert.assertNotNull(selfManagementPermittableGroup);
      Assert.assertEquals(PermittableGroupIds.SELF_MANAGEMENT, selfManagementPermittableGroup.getIdentifier());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void createWithIllegalMethodThrows() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String identifier = testEnvironment.generateUniqueIdentifer("group");

      final PermittableEndpoint permittableEndpoint = buildPermittableEndpoint();
      permittableEndpoint.setMethod("blah");
      final PermittableGroup group = buildPermittableGroup(identifier, permittableEndpoint);

      getTestSubject().createPermittableGroup(group);
      Assert.assertFalse("create should throw because 'blah' is an illegal method name.", true);
    }
  }

  @Test
  public void create() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String identifier = testEnvironment.generateUniqueIdentifer("group");

      final PermittableEndpoint permittableEndpoint = buildPermittableEndpoint();
      final PermittableGroup group = buildPermittableGroup(identifier, permittableEndpoint);

      getTestSubject().createPermittableGroup(group);

      {
        final boolean found = eventRecorder.wait(EventConstants.OPERATION_POST_PERMITTABLE_GROUP, group.getIdentifier());
        Assert.assertTrue(found);
      }

      final List<PermittableGroup> permittableGroups = getTestSubject().getPermittableGroups();
      Assert.assertTrue(Helpers.instancePresent(permittableGroups, PermittableGroup::getIdentifier, identifier));

      final PermittableGroup createdGroup = getTestSubject().getPermittableGroup(identifier);
      Assert.assertNotNull(createdGroup);
      Assert.assertEquals(createdGroup, group);
      Assert.assertEquals(Collections.singletonList(permittableEndpoint), createdGroup.getPermittables());
    }
  }

  private PermittableGroup buildPermittableGroup(final String identifier, final PermittableEndpoint permittableEndpoint) {
    final PermittableGroup ret = new PermittableGroup();
    ret.setIdentifier(identifier);
    ret.setPermittables(Collections.singletonList(permittableEndpoint));
    return ret;
  }

  private PermittableEndpoint buildPermittableEndpoint() {
    final PermittableEndpoint ret = new PermittableEndpoint();
    ret.setPath("/x/y/z");
    ret.setMethod("POST");
    return ret;
  }
}
