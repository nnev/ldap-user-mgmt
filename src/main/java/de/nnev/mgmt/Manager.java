package de.nnev.mgmt;

import com.unboundid.ldap.sdk.LDAPException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "nlm", mixinStandardHelpOptions = true, synopsisSubcommandLabel = "COMMAND")
public class Manager implements Runnable {

  @Spec CommandSpec spec;

  final ManagerLDAP u;

  public Manager() throws LDAPException {
    u = new ManagerLDAP(null);
  }

  public static void main(String... args) throws LDAPException {
    int exitCode = new CommandLine(new Manager()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    throw new ParameterException(spec.commandLine(), "Missing required subcommand");
  }

  @Command
  int addNonameUser(
      @Parameters(paramLabel = "username") String username,
      @Parameters(paramLabel = "realname") String realname)
      throws LDAPException {
    u.addUserWithUserGroup(username, realname, "/usr/bin/bash");

    return ExitCode.OK;
  }
}
