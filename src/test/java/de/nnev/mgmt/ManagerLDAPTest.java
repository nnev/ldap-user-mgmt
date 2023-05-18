package de.nnev.mgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManagerLDAPTest {

  InMemoryDirectoryServer srv;
  ManagerLDAP managerLDAP;

  @BeforeEach
  void startInMemServer() throws LDAPException, LDIFException, IOException {
    InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=noname-ev,dc=de");
    InputStream schemaStream =
        Objects.requireNonNull(ManagerLDAPTest.class.getResourceAsStream("schema.ldif"));
    Schema schema = Schema.getSchema(schemaStream);
    config.setSchema(schema);
    config.setEnforceSingleStructuralObjectClass(true);
    config.setEnforceAttributeSyntaxCompliance(true);
    config.setGenerateOperationalAttributes(true);
    srv = new InMemoryDirectoryServer(config);

    srv.add(
        "dc=noname-ev,dc=de",
        new Attribute("objectClass", "top", "dcObject", "organization"),
        new Attribute("dc", "noname-ev"),
        new Attribute("o", "noname-ev.de"));
    srv.add(
        "ou=administration,dc=noname-ev,dc=de",
        new Attribute("objectClass", "top", "organizationalUnit"),
        new Attribute("ou", "administration"));
    srv.add(
        "ou=bindusers,ou=administration,dc=noname-ev,dc=de",
        new Attribute("objectClass", "top", "organizationalUnit"),
        new Attribute("ou", "bindusers"));
    srv.add(
        "ou=sudoers,ou=administration,dc=noname-ev,dc=de",
        new Attribute("objectClass", "top", "organizationalUnit"),
        new Attribute("ou", "sudoers"));
    srv.add(
        "cn=Next POSIX UID,ou=administration,dc=noname-ev,dc=de",
        new Attribute("objectClass", "top", "uidNext"),
        new Attribute("cn", "Next POSIX UID"),
        new Attribute("uidNumber", "2000"));
    srv.add(
        "cn=Next POSIX GID,ou=administration,dc=noname-ev,dc=de",
        new Attribute("objectClass", "top", "GidNext"),
        new Attribute("cn", "Next POSIX GID"),
        new Attribute("gidNumber", "2000"));
    srv.add(
        "ou=users,dc=noname-ev,dc=de",
        new Attribute("objectClass", "top", "organizationalUnit"),
        new Attribute("ou", "users"));
    srv.add(
        "ou=groups,dc=noname-ev,dc=de",
        new Attribute("objectClass", "top", "organizationalUnit"),
        new Attribute("ou", "groups"));

    InputStream dataStream =
        Objects.requireNonNull(ManagerLDAPTest.class.getResourceAsStream("data.ldif"));

    srv.importFromLDIF(false, new LDIFReader(dataStream));

    srv.startListening();

    managerLDAP = new ManagerLDAP(srv.getConnection());
  }

  @AfterEach
  void stopInMemServer() {
    srv.shutDown(true);
  }

  @Test
  void test_getNextUIDNumber() throws LDAPException {
    assertEquals(2000, managerLDAP.getNextUidNumber());
    assertEquals(2001, managerLDAP.getNextUidNumber());
    assertEquals(2002, managerLDAP.getNextUidNumber());
  }

  @Test
  void test_getNextGIDNumber() throws LDAPException {
    assertEquals(2000, managerLDAP.getNextGidNumber());
    assertEquals(2001, managerLDAP.getNextGidNumber());
    assertEquals(2002, managerLDAP.getNextGidNumber());
  }

  @Test
  void test_getNextIDNumber() throws LDAPException {
    assertEquals(2000, managerLDAP.getNextUidNumber());
    assertEquals(2001, managerLDAP.getNextUidNumber());
    assertEquals(2002, managerLDAP.getNextUidNumber());
    assertEquals(2000, managerLDAP.getNextGidNumber());
    assertEquals(2001, managerLDAP.getNextGidNumber());
    assertEquals(2002, managerLDAP.getNextGidNumber());
  }

  @Test
  void test_checkIfUIDExits() throws LDAPException {
    assertTrue(managerLDAP.isUidUnique("user1"));
    assertTrue(managerLDAP.isUidUnique("user2"));
    assertTrue(managerLDAP.isUidUnique("user3"));
    assertFalse(managerLDAP.isUidUnique("user4"));
  }

  @Test
  void test_checkIfGIDExits() throws LDAPException {
    assertTrue(managerLDAP.isGidUnique("group1"));
    assertTrue(managerLDAP.isGidUnique("group2"));
    assertTrue(managerLDAP.isGidUnique("group3"));
    assertFalse(managerLDAP.isGidUnique("user1"));
    assertFalse(managerLDAP.isGidUnique("group4"));
  }
}
