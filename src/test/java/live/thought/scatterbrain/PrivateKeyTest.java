package live.thought.scatterbrain;

import static org.junit.Assert.*;

import org.junit.Test;

public class PrivateKeyTest
{

  @Test
  public void test()
  {
    PrivateKey testKey = new PrivateKey();
    assertNotNull(testKey);
    System.out.println(testKey);
  }

}
