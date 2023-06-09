package de.nnev.mgmt;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class TestInMemLdap {
  InMemoryDirectoryServer srv;

  public void startLdap() throws LDIFException, IOException, LDAPException {
    InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=noname-ev,dc=de");
    InputStream schemaStream = Objects.requireNonNull(TestInMemLdap.class.getResourceAsStream("schema.ldif"));
    Schema schema = Schema.getSchema(schemaStream);
    config.setSchema(schema);
    config.setEnforceSingleStructuralObjectClass(true);
    config.setEnforceAttributeSyntaxCompliance(true);
    config.setGenerateOperationalAttributes(true);
    srv = new InMemoryDirectoryServer(config);

    srv.add("dc=noname-ev,dc=de", new Attribute("objectClass", "top", "dcObject", "organization"), new Attribute("dc", "noname-ev"), new Attribute("o", "noname-ev.de"));
    srv.add("ou=administration,dc=noname-ev,dc=de", new Attribute("objectClass", "top", "organizationalUnit"), new Attribute("ou", "administration"));
    srv.add("ou=bindusers,ou=administration,dc=noname-ev,dc=de", new Attribute("objectClass", "top", "organizationalUnit"), new Attribute("ou", "bindusers"));
    srv.add("ou=sudoers,ou=administration,dc=noname-ev,dc=de", new Attribute("objectClass", "top", "organizationalUnit"), new Attribute("ou", "sudoers"));
    srv.add("cn=Next POSIX UID,ou=administration,dc=noname-ev,dc=de", new Attribute("objectClass", "top", "uidNext"), new Attribute("cn", "Next POSIX UID"), new Attribute("uidNumber", "2000"));
    srv.add("cn=Next POSIX GID,ou=administration,dc=noname-ev,dc=de", new Attribute("objectClass", "top", "GidNext"), new Attribute("cn", "Next POSIX GID"), new Attribute("gidNumber", "2000"));
    srv.add("ou=users,dc=noname-ev,dc=de", new Attribute("objectClass", "top", "organizationalUnit"), new Attribute("ou", "users"));
    srv.add("ou=groups,dc=noname-ev,dc=de", new Attribute("objectClass", "top", "organizationalUnit"), new Attribute("ou", "groups"));

    InputStream dataStream = Objects.requireNonNull(TestInMemLdap.class.getResourceAsStream("data.ldif"));

    srv.importFromLDIF(false, new LDIFReader(dataStream));

    srv.startListening();
  }

  public void shutdownLdap() {
    srv.shutDown(true);
  }

  public LDAPInterface getConnection() throws LDAPException {
    return srv.getConnection();
  }
}
