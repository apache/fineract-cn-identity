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

import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.lang.security.RsaKeyPairFactory;
import io.mifos.identity.api.v1.events.ApplicationSignatureEvent;
import io.mifos.identity.api.v1.events.EventConstants;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestApplications extends AbstractComponentTest {
  private String createTestApplicationName()
  {
    return "test" + RandomStringUtils.randomNumeric(3) + "-v1";
  }

  @Test
  public void testCreateApplicationSignature() throws InterruptedException {
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final String testApplicationName = createApplicationSignature();

      final List<String> foundApplications = getTestSubject().getApplications();
      Assert.assertTrue(foundApplications.contains(testApplicationName));
    }
  }

  @Test
  public void testDeleteApplication() throws InterruptedException {
    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final String testApplicationName = createApplicationSignature();

      getTestSubject().deleteApplication(testApplicationName);

      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_DELETE_APPLICATION, testApplicationName));

      final List<String> foundApplications = getTestSubject().getApplications();
      Assert.assertFalse(foundApplications.contains(testApplicationName));
    }
  }

  private String createApplicationSignature() throws InterruptedException {
    final String testApplicationName = createTestApplicationName();
    final RsaKeyPairFactory.KeyPairHolder keyPair = RsaKeyPairFactory.createKeyPair();
    final Signature signature = new Signature(keyPair.getPublicKeyMod(), keyPair.getPublicKeyExp());

    getTestSubject().setApplicationSignature(testApplicationName, keyPair.getTimestamp(), signature);

    Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE,
            new ApplicationSignatureEvent(testApplicationName, keyPair.getTimestamp())));
    return testApplicationName;
  }
}
