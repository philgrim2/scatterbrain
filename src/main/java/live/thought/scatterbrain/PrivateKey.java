package live.thought.scatterbrain;

import java.security.SecureRandom;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Base58;

public class PrivateKey
{
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final int KEY_SIZE = 32;
  private static final int KEY_VER = 123;
  private String key;
   
  public PrivateKey()
  {
    byte[] data = new byte[KEY_SIZE];
    secureRandom.nextBytes(data);
    Sha256Hash hash = Sha256Hash.of(data);
    key = Base58.encodeChecked(KEY_VER, hash.getBytes());
  }
  
  @Override
  public String toString()
  {
    return key;
  }
 
}
