package de.nnev.mgmt;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFException;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagerLDAPTest {

  TestInMemLdap inMemLdapManager;
  ManagerLDAP managerLDAP;

  @BeforeEach
  void startInMemServer() throws LDAPException, LDIFException, IOException {
    inMemLdapManager = new TestInMemLdap();
    inMemLdapManager.startLdap();
    managerLDAP = new ManagerLDAP(inMemLdapManager.getConnection());
  }

  @AfterEach
  void stopInMemServer() {
    inMemLdapManager.shutdownLdap();
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
