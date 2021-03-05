package live.thought.scatterbrain;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import live.thought.thought4j.ThoughtRPCClient;

public class Scatterbrain
{
  /** RELEASE VERSION */
  public static final String               VERSION               = "v0.1";
  /** Options for the command line parser. */
  protected static final Options           options               = new Options();
  /** The Commons CLI command line parser. */
  protected static final CommandLineParser gnuParser             = new GnuParser();
  /** Default values for connection. */
  private static final String              DEFAULT_HOST          = "localhost";
  private static final String              DEFAULT_PORT          = "10617";
  private static final String              DEFAULT_USER          = "user";
  private static final String              DEFAULT_PASS          = "password";
  private static final String              DEFAULT_PREFIX        = "scatterbrain";
  private static final String              DEFAULT_SCATTER       = Integer.toString(10);
  private static final String              DEFAULT_INTERVAL      = Integer.toString(120);                 // 2 minutes
  private static final String              DEFAULT_STAKE_MEAN    = Double.toString(50.0);
  private static final String              DEFAULT_STAKE_SD      = Double.toString(5);
  private static final String              DEFAULT_STAKE_THRESH  = Double.toString(10.0);
  private static final String              DEFAULT_TRANSFER_MIN  = Double.toString(0.01);
  private static final String              DEFAULT_TRANSFER_MAX  = Double.toString(0.25);
  private static final String              DEFAULT_MINB          = Double.toString(315000.0);             // Stay above
                                                                                                          // masternode
                                                                                                          // stake by
                                                                                                          // default

  private static final String              HOST_PROPERTY         = "host";
  private static final String              PORT_PROPERTY         = "port";
  private static final String              USER_PROPERTY         = "user";
  private static final String              PASS_PROPERTY         = "password";
  private static final String              ACCOUNT_PROPERTY      = "account";
  private static final String              SCATTER_PROPERTY      = "scatter";
  private static final String              PREFIX_PROPERTY       = "prefix";
  private static final String              INTR_PROPERTY         = "interval";
  private static final String              STAKE_MEAN_PROPERTY   = "stakeMean";
  private static final String              STAKE_SD_PROPERTY     = "stakeSD";
  private static final String              STAKE_THRESH_PROPERTY = "stakeThreshhold";
  private static final String              TXFR_MIN_PROPERTY     = "transferMin";
  private static final String              TXFR_MAX_PROPERTY     = "transferMax";
  private static final String              MINB_PROPERTY         = "minimum";
  private static final String              HELP_OPTION           = "help";
  private static final String              CONFIG_OPTION         = "config";

  private static Random                    random                = new Random(System.currentTimeMillis());

  /** Connection for Thought daemon */
  private ThoughtRPCClient                 client;

  private boolean                          testnet;
  private int                              scatter;
  private String                           prefix;
  private String                           stakeAccount;
  private int                              interval;
  private double                           stakeMean;
  private double                           stakeSD;
  private double                           stakeThresh;
  private double                           transferMin;
  private double                           transferMax;
  private double                           minBalance;

  /** Set up command line options. */
  static
  {
    options.addOption("h", HOST_PROPERTY, true, "Thought RPC server host (default: localhost)");
    options.addOption("P", PORT_PROPERTY, true, "Thought RPC server port (default: 10617)");
    options.addOption("u", USER_PROPERTY, true, "Thought server RPC user");
    options.addOption("p", PASS_PROPERTY, true, "Thought server RPC password");
    options.addOption("a", ACCOUNT_PROPERTY, true, "Thought wallet account name to obtain stake from");
    options.addOption("S", SCATTER_PROPERTY, true, "Number of scatter accounts to use (default: 10)");
    options.addOption("x", PREFIX_PROPERTY, true, "Thought wallet account name prefix for scatter accounts (default: scatterbrain)");
    options.addOption("i", INTR_PROPERTY, true, "Interval in seconds between rounds (default: 2 minutes)");
    options.addOption("s", STAKE_MEAN_PROPERTY, true, "Mean amount to transfer as a stake to a scatter account (default: 50.0)");
    options.addOption("d", STAKE_SD_PROPERTY, true, "Standard deviation for stake transfer distribution (default: 5.0)");
    options.addOption("n", STAKE_THRESH_PROPERTY, true, "Threshhold amount below which a scatter account will receive new stake. (default: 10.0)");
    options.addOption("t", TXFR_MIN_PROPERTY, true, "Minimum limit of scatter amount. (default: 0.01");
    options.addOption("T", TXFR_MAX_PROPERTY, true, "Maximum limit of scatter amount (default: 0.25");
    options.addOption("m", MINB_PROPERTY, true,
        "Minimum balance to keep in the source account. (Defaults to 315,000.0)");
    options.addOption("H", HELP_OPTION, true, "Displays usage information");
    options.addOption("f", CONFIG_OPTION, true,
        "Configuration file to load options from.  Command line options override config file.");
  }

  public Scatterbrain(Properties props)
  {
    String host = props.getProperty(HOST_PROPERTY, DEFAULT_HOST);
    int    port = Integer.parseInt(props.getProperty(PORT_PROPERTY, DEFAULT_PORT));
    String user = props.getProperty(USER_PROPERTY, DEFAULT_USER);
    String pass = props.getProperty(PASS_PROPERTY, DEFAULT_PASS);
    prefix = props.getProperty(PREFIX_PROPERTY, DEFAULT_PREFIX);
    scatter = Integer.parseInt(props.getProperty(SCATTER_PROPERTY, DEFAULT_SCATTER));
    stakeAccount = props.getProperty(ACCOUNT_PROPERTY);
    interval = Integer.parseInt(props.getProperty(INTR_PROPERTY, DEFAULT_INTERVAL));
    stakeMean = Double.parseDouble(props.getProperty(STAKE_MEAN_PROPERTY, DEFAULT_STAKE_MEAN));
    stakeSD = Double.parseDouble(props.getProperty(STAKE_SD_PROPERTY, DEFAULT_STAKE_SD));
    stakeThresh = Double.parseDouble(props.getProperty(STAKE_THRESH_PROPERTY, DEFAULT_STAKE_THRESH));
    transferMin = Double.parseDouble(props.getProperty(TXFR_MIN_PROPERTY, DEFAULT_TRANSFER_MIN));
    transferMax = Double.parseDouble(props.getProperty(TXFR_MAX_PROPERTY, DEFAULT_TRANSFER_MAX));
    minBalance = Double.parseDouble(props.getProperty(MINB_PROPERTY, DEFAULT_MINB));
    
    if (props.getProperty(PORT_PROPERTY, DEFAULT_PORT).startsWith("10"))
    {
      testnet = false;
    }
    else
    {
      testnet = true;
    }

    URL url = null;
    try
    {
      url = new URL("http://" + user + ':' + pass + "@" + host + ":" + port + "/");
      client = new ThoughtRPCClient(url);
    }
    catch (MalformedURLException e)
    {
      throw new IllegalArgumentException("Invalid URL: " + url);
    }
  }

  public void run()
  {
    boolean moreElectricity = true;

    while (moreElectricity)
    {
      double balance = client.getBalance(stakeAccount, 6);
      Console.output("Current stake account balance: " + balance);
      if (balance > minBalance)
      {
        int scatters = 0;
        double scatterAmount = 0.0;
        int stakes = 0;
        double stakeAmount = 0.0;
        for (int i = 1; i <= scatter; i++)
        {
          String currentSource = String.format("%s-%04d", prefix, i);
          List<String> accountAddrs = client.getAddressesByAccount(currentSource);
          if (null == accountAddrs || accountAddrs.size() == 0)
          {
            // Account doesn't exist yet so create it.
            PrivateKey accountKey = new PrivateKey(testnet);
            client.importPrivKey(accountKey.toString(), currentSource, false);
            accountAddrs = client.getAddressesByAccount(currentSource);
            Console.output(String.format("@|cyan Created new scatter account %s |@", currentSource));
          }
            
          double curbal = client.getBalance(currentSource, 0);
          if (curbal < stakeThresh)
          {
            // Account is low, so stake it.
            double transfer = BigDecimal.valueOf(random.nextGaussian() * stakeSD + stakeMean).setScale(8, RoundingMode.HALF_UP)
                .doubleValue();
            String stakeTo = accountAddrs.get(0);
            try
            {
              client.sendFrom(stakeAccount, stakeTo, transfer, 6);
              Console.output(String.format("@|green Staked %f THT to %s. |@", transfer, currentSource));
              stakes++;
              stakeAmount += transfer;
            }
            catch (Exception e)
            {
              Console.output(String.format("@|red Exception sending stake transaction: %s |@", e.toString()));
            }
          }
          
          curbal = client.getBalance(currentSource, 6);
          if (curbal > 0)
          {
            // Account has some spendable coin, so pick a random scatter account to send to.
            int rando = random.nextInt(scatter - 1) + 1;
            while (rando == i)
            {
              // Don't send it right back to the sender.
              rando = random.nextInt(scatter - 1) + 1;
            }
            String currentTarget = String.format("%s-%04d", prefix, rando);
            List<String> targetAddrs = client.getAddressesByAccount(currentTarget);
            if (null == targetAddrs || targetAddrs.size() == 0)
            {
              // Target doesn't exist yet so create it.
              PrivateKey accountKey = new PrivateKey(testnet);
              client.importPrivKey(accountKey.toString(), currentTarget, false);
              targetAddrs = client.getAddressesByAccount(currentTarget);
              Console.output(String.format("@|cyan Created new scatter account %s |@", currentTarget));
            }
            double sct = Math.abs(random.nextDouble() * transferMax - transferMin) + transferMin;
            if (sct < curbal)
            {
              double send = BigDecimal.valueOf(sct).setScale(8, RoundingMode.HALF_UP).doubleValue();
              String sendTo = targetAddrs.get(0);
              try
              {
                client.sendFrom(currentSource, sendTo, send, 6);
                Console.output(String.format("@|green Scattered %f THT to %s. |@", send, currentTarget));
                scatters++;
                scatterAmount += send;
              }
              catch (Exception e)
              {
                Console.output(String.format("@|red Exception sending scatter transaction: %s |@", e.toString()));
              }
            }
            else
            {
              Console.output(String.format("@|magenta %s waiting for sufficient spendable coins. |@", currentSource));
            }
          }
          else
          {
            Console.output(String.format("@|magenta %s waiting for spendable coins. |@", currentSource));
          }
        }
        Console.output("@|yellow Scatter round complete. |@");
        Console.output(String.format("@|yellow Staked %f THT to %d accounts. |@", stakeAmount, stakes));
        Console.output(String.format("@|yellow Scattered %f THT to %d accounts. |@", scatterAmount, scatters));   
      }
      else
      {
        Console.output("Stake account spendable balance at or below minumum.  No scattering this cycle.");
      }
      try
      {
        Thread.sleep(interval * 1000);
      }
      catch (InterruptedException e)
      {
        Console.output("Who has disturbed my slumber?");
      }
    }

  }

  protected static void usage()
  {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Scatterbrain", options);
  }

  public static void main(String[] args)
  {

    CommandLine commandLine = null;

    try
    {
      Properties props = new Properties();
      // Read the command line
      commandLine = gnuParser.parse(options, args);
      // Check for the help option
      if (commandLine.hasOption(HELP_OPTION))
      {
        usage();
        System.exit(0);
      }
      // Check for a config file specified on the command line
      if (commandLine.hasOption(CONFIG_OPTION))
      {
        try
        {
          props.load(new FileInputStream(new File(commandLine.getOptionValue(CONFIG_OPTION))));
        }
        catch (Exception e)
        {
          Console.output(String.format("@|red Specified configuration file %s unreadable or not found.|@",
              commandLine.getOptionValue(CONFIG_OPTION)));
          System.exit(1);
        }
      }
      // Command line options override config file values
      if (commandLine.hasOption(HOST_PROPERTY))
      {
        props.setProperty(HOST_PROPERTY, commandLine.getOptionValue(HOST_PROPERTY));
      }
      if (commandLine.hasOption(PORT_PROPERTY))
      {
        props.setProperty(PORT_PROPERTY, commandLine.getOptionValue(PORT_PROPERTY));
      }
      if (commandLine.hasOption(USER_PROPERTY))
      {
        props.setProperty(USER_PROPERTY, commandLine.getOptionValue(USER_PROPERTY));
      }
      if (commandLine.hasOption(PASS_PROPERTY))
      {
        props.setProperty(PASS_PROPERTY, commandLine.getOptionValue(PASS_PROPERTY));
      }
      if (commandLine.hasOption(ACCOUNT_PROPERTY))
      {
        props.setProperty(ACCOUNT_PROPERTY, commandLine.getOptionValue(ACCOUNT_PROPERTY));
      }
      if (commandLine.hasOption(SCATTER_PROPERTY))
      {
        props.setProperty(SCATTER_PROPERTY, commandLine.getOptionValue(SCATTER_PROPERTY));
      }
      if (commandLine.hasOption(PREFIX_PROPERTY))
      {
        props.setProperty(PREFIX_PROPERTY, commandLine.getOptionValue(PREFIX_PROPERTY));
      }
      if (commandLine.hasOption(INTR_PROPERTY))
      {
        props.setProperty(INTR_PROPERTY, commandLine.getOptionValue(INTR_PROPERTY));
      }
      if (commandLine.hasOption(STAKE_MEAN_PROPERTY))
      {
        props.setProperty(STAKE_MEAN_PROPERTY, commandLine.getOptionValue(STAKE_MEAN_PROPERTY));
      }
      if (commandLine.hasOption(STAKE_SD_PROPERTY))
      {
        props.setProperty(STAKE_SD_PROPERTY, commandLine.getOptionValue(STAKE_SD_PROPERTY));
      }
      if (commandLine.hasOption(TXFR_MIN_PROPERTY))
      {
        props.setProperty(TXFR_MIN_PROPERTY, commandLine.getOptionValue(TXFR_MIN_PROPERTY));
      }
      if (commandLine.hasOption(TXFR_MAX_PROPERTY))
      {
        props.setProperty(TXFR_MAX_PROPERTY, commandLine.getOptionValue(TXFR_MAX_PROPERTY));
      }
      if (commandLine.hasOption(MINB_PROPERTY))
      {
        props.setProperty(MINB_PROPERTY, commandLine.getOptionValue(MINB_PROPERTY));
      }
      String account = props.getProperty(ACCOUNT_PROPERTY);
      if (null == account)
      {
        Console.output("@|red No Thought stake account specified.|@");
        usage();
        System.exit(1);
      }

      Scatterbrain sb = new Scatterbrain(props);
      sb.run();
      Console.end();
    }
    catch (ParseException pe)
    {
      System.err.println(pe.getLocalizedMessage());
      usage();
    }
    catch (Exception e)
    {
      System.err.println(e.getLocalizedMessage());
      e.printStackTrace(System.err);
    }
  }
}
