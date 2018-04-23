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

import org.apache.fineract.cn.anubis.api.v1.RoleConstants;
import org.apache.fineract.cn.anubis.api.v1.domain.ApplicationSignatureSet;
import org.apache.fineract.cn.anubis.api.v1.domain.Signature;
import org.apache.fineract.cn.anubis.token.SystemAccessTokenSerializer;
import org.apache.fineract.cn.api.context.AutoSeshat;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.apache.fineract.cn.api.util.InvalidTokenException;
import org.apache.fineract.cn.identity.api.v1.client.IdentityManager;
import org.apache.fineract.cn.lang.AutoTenantContext;
import org.apache.fineract.cn.lang.TenantContextHolder;
import org.apache.fineract.cn.test.env.TestEnvironment;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.concurrent.TimeUnit;

/**
 * @author Myrle Krantz
 */
public class TestProvisioning extends AbstractComponentTest {

  @Test
  public void testBoundaryInitializeCases() throws InterruptedException {
    final IdentityManager testSubject = getTestSubject();


    final ApplicationSignatureSet firstTenantSignatureSet;
    final Signature firstTenantIdentityManagerSignature;

    //Create tenant keyspaces.
    final String tenant1 = TestEnvironment.getRandomTenantName();
    final String tenant2 = TestEnvironment.getRandomTenantName();
    cassandraInitializer.initializeTenant(tenant1);
    cassandraInitializer.initializeTenant(tenant2);
    TimeUnit.SECONDS.sleep(1);
    // This gives cassandra a chance to complete saving the new keyspaces.
    // Theoretically, the creation of keyspaces is synchronous, but I've
    // found that the cassandra driver needs just a little bit longer
    // To show up in the request for metadata for that keyspace.


    try (final AutoTenantContext ignored = new AutoTenantContext(tenant1)) {

      final String invalidSeshatToken = "notBearer";
      try (final AutoSeshat ignored2 = new AutoSeshat(invalidSeshatToken)){
        testSubject.initialize(TestEnvironment.encodePassword(ADMIN_PASSWORD));
        Assert.fail("The key had the wrong format.  This should've failed.");
      }
      catch (final InvalidTokenException ignored2)
      {
      }


      final String wrongSystemToken = systemTokenFromWrongKey();
      try (final AutoSeshat ignored2 = new AutoSeshat(wrongSystemToken)){
        testSubject.initialize(TestEnvironment.encodePassword(ADMIN_PASSWORD));
        Assert.fail("The key was signed by the wrong source.  This should've failed.");
      }
      catch (final Exception e)
      {
        Assert.assertTrue("The exception should be 'invalid token'", (e instanceof InvalidTokenException));
      }


      try (final AutoUserContext ignored2 = tenantApplicationSecurityEnvironment.createAutoSeshatContext("goober")) {
        testSubject.initialize(TestEnvironment.encodePassword(ADMIN_PASSWORD));
        Assert.fail("The key was intended for a different tenant.  This should've failed.");
      }
      catch (final Exception e)
      {
        Assert.assertTrue("The exception should be 'not found'", (e instanceof InvalidTokenException));
      }

      // The second otherwise valid call to initialize for the same tenant should
      // not fail even though the tenant is now already initialized.
      try (final AutoUserContext ignored2 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
        firstTenantSignatureSet = testSubject.initialize(TestEnvironment.encodePassword(ADMIN_PASSWORD));

        final Signature applicationSignature = tenantApplicationSecurityEnvironment.getAnubis().getApplicationSignature(firstTenantSignatureSet.getTimestamp());
        firstTenantIdentityManagerSignature = tenantApplicationSecurityEnvironment.getAnubis().getSignatureSet(firstTenantSignatureSet.getTimestamp()).getIdentityManagerSignature();
        Assert.assertEquals(applicationSignature, firstTenantIdentityManagerSignature);

        testSubject.initialize("golden_osiris");
      }
    }


    final ApplicationSignatureSet secondTenantSignatureSet;
    try (final AutoTenantContext ignored = new AutoTenantContext(tenant2)) {
      try (final AutoUserContext ignored2
                   = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
        secondTenantSignatureSet = testSubject.initialize(TestEnvironment.encodePassword(ADMIN_PASSWORD));
        final Signature secondTenantIdentityManagerSignature = tenantApplicationSecurityEnvironment.getAnubis().getApplicationSignature(secondTenantSignatureSet.getTimestamp());
        Assert.assertNotEquals(firstTenantIdentityManagerSignature, secondTenantIdentityManagerSignature);
      }
    }
    catch (final Exception e)
    {
      Assert.fail("Call to initialize for a second tenant should succeed. "
          + "The exception was " + e
      );
      throw e;
    }



    TenantContextHolder.clear();
  }

  private String systemTokenFromWrongKey()
  {
    final SystemAccessTokenSerializer.Specification tokenSpecification
        = new SystemAccessTokenSerializer.Specification();

    tokenSpecification.setKeyTimestamp("rando");
    tokenSpecification.setPrivateKey(getWrongPrivateKey());

    tokenSpecification.setRole(RoleConstants.SYSTEM_ADMIN_ROLE_IDENTIFIER);
    tokenSpecification.setSecondsToLive(TimeUnit.HOURS.toSeconds(12L));
    tokenSpecification.setTargetApplicationName(APP_NAME);
    tokenSpecification.setTenant(TenantContextHolder.checkedGetIdentifier());

    return new SystemAccessTokenSerializer().build(tokenSpecification).getToken();
  }


  private PrivateKey getWrongPrivateKey() {
    try {
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      final BigInteger privateKeyMod = new BigInteger("17492023076590407419772677529630320569634203626210733433914657600705550392421401008213478702016995697617177968917710500448063776244435761300358170637857566629780506514059676334317863416316403254208652514809444684705031748559737773841335114470369449872988726825545007588731959817361831095583721775522968972071928027030514641182819255368960492742269021132488312466659639538013906582095129294788911611410130509557024329936361580892139238423117992298190557606490543083859770282260174239092737213765902825945545746379786237952115129023474946280230282545899883335448866567923667432417504919606306921621754480829178419392063");
      final BigInteger privateKeyExp = new BigInteger("3836197074627064495542864246660307880240969356539464297200899853440665208817504565223497099105700278649491111086168927826113808321425686210385705579717210204462139251515628239821027066889171978771395739740240603830895850009141569242130546108040040023566336125601696661013541334741315567340965150672011734372736240827969821590544366269533567400051316301569296349329670063250330460924547069022975441956699127698164632663315582302411984903513839691646332895582584509587803859003388718326640891827257180737700763719907116123118603418352134643169731657061459925351503055596019271348089711003706283690698717182672701958953");
      final RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(privateKeyMod, privateKeyExp);

      return keyFactory.generatePrivate(privateKeySpec);

    } catch (final InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
