package de.nnev.mgmt;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldif.LDIFException;
import picocli.CommandLine;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "num", mixinStandardHelpOptions = true, synopsisSubcommandLabel = "COMMAND")
public class Manager implements Runnable {

  @CommandLine.Option(names = {"-t", "--test"}, description = "Enable use of local test ldap server", scope = CommandLine.ScopeType.INHERIT)
  boolean debugMode = false;

  @Spec
  CommandSpec spec;

  ManagerLDAP u;

  TestInMemLdap testInMemLdap;

  private int executionStrategy(ParseResult parseResult) {
    init(); // custom initialization to be done before executing any command or subcommand
    int retCode = new CommandLine.RunLast().execute(parseResult); // default execution strategy
    shutdown();
    return retCode;
  }

  private void init() {
    try {
      if (debugMode) {
        testInMemLdap = new TestInMemLdap();
        testInMemLdap.startLdap();
        u = new ManagerLDAP(testInMemLdap.getConnection());
      } else {
        u = new ManagerLDAP(ManagerLDAP.getLDAPiConnection());
      }
    } catch (IOException | LDIFException e) {
      throw new RuntimeException("Unknown error occurred", e);
    } catch (LDAPException e) {
      if (e.getResultCode() == ResultCode.CONNECT_ERROR) {
        throw new RuntimeException("Unable to connect to ldap server", e);
      }
      throw new RuntimeException("Unknown error occurred", e);
    }
  }

  private void shutdown() {
    if (testInMemLdap != null) {
      try {
        testInMemLdap.srv.exportToLDIF("/tmp/ldapDump.ldif", true, true);
      } catch (LDAPException ignored) {
      }

      testInMemLdap.shutdownLdap();
    }
  }

  public static void main(String... args) {
    var app = new Manager();
    int exitCode = new CommandLine(app).setExecutionStrategy(app::executionStrategy).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    throw new ParameterException(spec.commandLine(), "Missing required subcommand");
  }

  @Command
  int addNonameUser(@Parameters(paramLabel = "username") String username, @Parameters(paramLabel = "realname") String realname) throws LDAPException {
    u.addUserWithUserGroup(username, realname, "/usr/bin/bash");
    u.addUserToGroup(username, "noname");

    System.out.println(Help.Ansi.AUTO.string("@|bold,green User successfully created with user group and added to the noname group!|@"));

    return ExitCode.OK;
  }

  @Command
  int addUserToGroup(@Parameters(paramLabel = "username") String username, @Parameters(paramLabel = "group") String group) throws LDAPException {
    u.addUserToGroup(username, group);

    System.out.println(Help.Ansi.AUTO.string("@|bold,green User successfully created and added to the noname group!|@"));

    return ExitCode.OK;
  }

  @Command
  int removeUserFromGroup(@Parameters(paramLabel = "username") String username, @Parameters(paramLabel = "group") String group) throws LDAPException {
    u.removeUserFromGroup(username, group);

    System.out.println(Help.Ansi.AUTO.string("@|bold,green User successfully created and added to the noname group!|@"));

    return ExitCode.OK;
  }

  @Command
  int addSshKey(@Parameters(paramLabel = "username") String username, @Parameters(paramLabel = "fileToPublicKey") Path sshPublicKey) throws IOException, LDAPException {
    String publicKey = Files.readString(sshPublicKey, StandardCharsets.UTF_8);

    String[] keys = publicKey.split("\n");
    for (String key : keys) {
      if (key.length() > 0) {
        u.addSshKey(username, key);
        System.out.println(
          Help.Ansi.AUTO.string("@|bold,green Key added successfully to user " + username + ":|@ @|underline " + key + "|@")
        );
      }
    }

    return ExitCode.OK;
  }

  @Command
  int removeSshKey(@Parameters(paramLabel = "username") String username, @Parameters(paramLabel = "fileToPublicKey") Path sshPublicKey) throws IOException, LDAPException {
    String publicKey = Files.readString(sshPublicKey, StandardCharsets.UTF_8);

    String[] keys = publicKey.split("\n");
    for (String key : keys) {
      if (key.length() > 0) {
        u.removeSshKey(username, key);
        System.out.println(
          Help.Ansi.AUTO.string("@|bold,green Key removed successfully from user " + username + " if existed:|@ @|underline " + key + " |@")
        );
      }
    }

    return ExitCode.OK;
  }

  @Command
  int getSshKeys(@Parameters(paramLabel = "username") String username) throws LDAPSearchException {
    var user = u.getUserEntry(username);
    String[] keys = user.getAttributeValues("sshPublicKey");
    if (keys != null && keys.length > 0) {
      System.out.println(
        Help.Ansi.AUTO.string("@|bold,green Found the following " + keys.length + " keys for user " + username + ":|@")
      );
      for (String key : keys) {
        System.out.println(key);
      }
    } else {
      System.out.println(
        Help.Ansi.AUTO.string("@|bold,yellow No keys found for user " + username + "!|@")
      );
    }

    return ExitCode.OK;
  }
}
