import io.mifos.anubis.api.v1.client.Anubis;
import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.core.api.context.AutoSeshat;
import io.mifos.core.api.util.NotFoundException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Myrle Krantz
 */
public class TestKeyRotation extends AbstractComponentTest {
  @Test
  public void testKeyRotation()
  {
    final Anubis anubis = tenantApplicationSecurityEnvironment.getAnubis();

    final String systemToken = tenantApplicationSecurityEnvironment.getSystemSecurityEnvironment().systemToken(APP_NAME);

    try (final AutoSeshat ignored1 = new AutoSeshat(systemToken)) {
      //Create a signature set then test that it is listed.
      final String timestamp = getTestSubject().createSignatureSet();
      {
        final List<String> signatureSets = anubis.getAllSignatureSets();
        Assert.assertTrue(signatureSets.contains(timestamp));
      }

      //For identity, application signature and identity manager signature should be identical.
      final ApplicationSignatureSet signatureSet = anubis.getSignatureSet(timestamp);
      Assert.assertEquals(signatureSet.getApplicationSignature(), signatureSet.getIdentityManagerSignature());

      final Signature applicationSignature = anubis.getApplicationSignature(timestamp);
      Assert.assertEquals(signatureSet.getApplicationSignature(), applicationSignature);

      //Create a second signature set and test that it and the previous signature set are listed.
      final String timestamp2 = getTestSubject().createSignatureSet();
      {
        final List<String> signatureSets = anubis.getAllSignatureSets();
        Assert.assertTrue(signatureSets.contains(timestamp));
        Assert.assertTrue(signatureSets.contains(timestamp2));
      }

      //Get the newly created signature set, and test that its contents are correct.
      final ApplicationSignatureSet signatureSet2 = anubis.getSignatureSet(timestamp2);
      Assert.assertEquals(signatureSet2.getApplicationSignature(), signatureSet2.getIdentityManagerSignature());

      //Delete one of the signature sets and test that it is no longer listed.
      anubis.deleteSignatureSet(timestamp);
      {
        final List<String> signatureSets = anubis.getAllSignatureSets();
        Assert.assertFalse(signatureSets.contains(timestamp));
      }

      //Getting the newly deleted signature set should fail.
      try {
        anubis.getSignatureSet(timestamp);
        Assert.fail("Not found exception should be thrown.");
      } catch (final NotFoundException ignored) {
      }

      //Getting the newly deleted application signature set should likewise fail.
      try {
        anubis.getApplicationSignature(timestamp);
        Assert.fail("Not found exception should be thrown.");
      } catch (final NotFoundException ignored) {
      }
    }
  }
}
