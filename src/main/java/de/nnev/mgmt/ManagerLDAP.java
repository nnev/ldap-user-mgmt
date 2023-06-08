package de.nnev.mgmt;

import com.unboundid.ldap.sdk.*;
import org.newsclub.net.unix.AFUNIXSocketFactory;

import javax.net.SocketFactory;

import static com.unboundid.ldap.sdk.ModificationType.ADD;
import static com.unboundid.ldap.sdk.ModificationType.DELETE;
import static com.unboundid.ldap.sdk.SearchScope.SUB;

public class ManagerLDAP {

  public static final String LDAPI_HOST = "localhost";
  public static final int LDAPI_PORT = 5000;
  public static final String OPENLDAP_UNIX_SOCKET = "/var/run/slapd/ldapi";
  public static final String OPENLDAP_UNIX_AUTHZ_ID = "";

  public static final String LDAP_BASE = "dc=noname-ev,dc=de";
  public static final String USERS_BASE = "ou=users," + LDAP_BASE;
  public static final String GROUPS_BASE = "ou=groups," + LDAP_BASE;

  public static final String UID_DN = "cn=Next POSIX UID,ou=administration," + LDAP_BASE;
  public static final String GID_DN = "cn=Next POSIX GID,ou=administration," + LDAP_BASE;

  private final LDAPInterface ldap;

  public ManagerLDAP(LDAPInterface ldap) {
    this.ldap = ldap;
  }

  public static LDAPConnection getLDAPiConnection() throws LDAPException {
    SocketFactory sf = new AFUNIXSocketFactory.FactoryArg(OPENLDAP_UNIX_SOCKET);

    var conn = new LDAPConnection(sf, null, LDAPI_HOST, LDAPI_PORT);
    conn.bind(new EXTERNALBindRequest(OPENLDAP_UNIX_AUTHZ_ID));

    return conn;
  }

  public int addUser(String uid, String name, String gid, String shell, String home)
    throws LDAPException {
    int gidNumber = getGidNumber(gid);
    return addUser(uid, name, gidNumber, shell, home);
  }

  public int addUser(String uid, String name, int gidNumber, String shell, String home)
    throws LDAPException {
    checkPosixUid(uid);
    checkPosixUidUnique(uid);

    int uidNumber = getNextUidNumber();
    DN userDN = new DN(new RDN("uid", uid), new DN(USERS_BASE));
    Entry user =
      new Entry(
        userDN,
        new Attribute("objectClass", "top", "account", "posixAccount", "ldapPublicKey"),
        new Attribute("cn", name),
        new Attribute("uid", uid),
        new Attribute("uidNumber", String.valueOf(uidNumber)),
        new Attribute("gidNumber", String.valueOf(gidNumber)),
        new Attribute("loginShell", shell),
        new Attribute("homeDirectory", home));

    ldap.add(user);

    return uidNumber;
  }

  public int addUserWithUserGroup(String uid, String name, String shell) throws LDAPException {
    // check valid if its a valid user name
    checkPosixUid(uid);
    checkPosixUidUnique(uid);

    int gidNumber = addGroup(uid);

    return addUser(uid, name, gidNumber, shell, "/home/" + uid);
  }

  public int addGroup(String gid) throws LDAPException {
    checkPosixGid(gid);
    checkPosixGidUnique(gid);

    int gidNumber = getNextGidNumber();

    DN groupDN = new DN(new RDN("cn", gid), new DN(GROUPS_BASE));
    Entry group =
      new Entry(
        groupDN,
        new Attribute("objectClass", "top", "groupOfEntries", "posixGroup"),
        new Attribute("cn", gid),
        new Attribute("gidNumber", String.valueOf(gidNumber)));

    ldap.add(group);

    return gidNumber;
  }

  public void addUserToGroup(String uid, String gid) throws LDAPException {
    Entry user = getUserEntry(uid);
    Entry groupBefore = getGroupEntry(gid);
    Entry groupAfter = groupBefore.duplicate();
    groupAfter.addAttribute("member", user.getDN());
    var mods = Entry.diff(groupBefore, groupAfter, true, true);
    if (mods.size() > 0) {
      ldap.modify(new ModifyRequest(groupBefore.getDN(), mods));
    }
  }

  public void removeUserFromGroup(String uid, String gid) throws LDAPException {
    Entry user = getUserEntry(uid);
    Entry groupBefore = getGroupEntry(gid);
    Entry groupAfter = groupBefore.duplicate();
    groupAfter.removeAttributeValue("member", user.getDN());
    var mods = Entry.diff(groupBefore, groupAfter, true, true);
    if (mods.size() > 0) {
      ldap.modify(new ModifyRequest(groupBefore.getDN(), mods));
    }
  }

  public static boolean validatePosixId(String id) {
    return id.matches("^[a-z_][a-z0-9_-]{0,31}$");
  }

  public static void checkPosixUid(String uid) {
    if (!validatePosixId(uid)) {
      throw new RuntimeException("Invalid user name");
    }
  }

  public static void checkPosixGid(String gid) {
    if (!validatePosixId(gid)) {
      throw new RuntimeException("Invalid group name");
    }
  }

  public Entry getUserEntry(String uid) throws LDAPSearchException {
    var res =
      ldap.search(
        ManagerLDAP.USERS_BASE,
        SUB,
        Filter.createANDFilter(
          Filter.createEqualityFilter("objectClass", "account"),
          Filter.createEqualityFilter("objectClass", "posixAccount"),
          Filter.createEqualityFilter("uid", uid)));

    switch (res.getEntryCount()) {
      case 0 -> throw new RuntimeException("User not found");
      case 1 -> {
        return res.getSearchEntries().get(0);
      }
      default -> throw new RuntimeException("User not unique / general error");
    }
  }

  public Entry getGroupEntry(String gid) throws LDAPSearchException {
    var res =
      ldap.search(
        ManagerLDAP.GROUPS_BASE,
        SUB,
        Filter.createANDFilter(
          Filter.createEqualityFilter("objectClass", "groupOfEntries"),
          Filter.createEqualityFilter("objectClass", "posixGroup"),
          Filter.createEqualityFilter("cn", gid)));

    switch (res.getEntryCount()) {
      case 0 -> throw new RuntimeException("Group not found");
      case 1 -> {
        return res.getSearchEntries().get(0);
      }
      default -> throw new RuntimeException("Group not unique / general error");
    }
  }

  public int getGidNumber(String gid) throws LDAPSearchException {
    Entry entry = getGroupEntry(gid);
    return entry.getAttributeValueAsInteger("gidNumber");
  }

  public int getNextUidNumber() throws LDAPException {
    return getNextPosixIdNumber(ManagerLDAP.UID_DN, "uidNumber");
  }

  public int getNextGidNumber() throws LDAPException {
    return getNextPosixIdNumber(ManagerLDAP.GID_DN, "gidNumber");
  }

  public int getNextPosixIdNumber(String dn, String attributeName) throws LDAPException {
    var entry = ldap.getEntry(dn, attributeName);
    int nextID = entry.getAttributeValueAsInteger(attributeName);

    ldap.modify(
      dn,
      new Modification(DELETE, attributeName, String.valueOf(nextID)),
      new Modification(ADD, attributeName, String.valueOf(nextID + 1)));

    return nextID;
  }

  public boolean isUidUnique(String uid) throws LDAPSearchException {
    var res =
      ldap.search(
        ManagerLDAP.USERS_BASE,
        SUB,
        Filter.createANDFilter(
          Filter.createEqualityFilter("objectClass", "account"),
          Filter.createEqualityFilter("objectClass", "posixAccount"),
          Filter.createEqualityFilter("uid", uid)));
    return res.getEntryCount() > 0;
  }

  public void checkPosixUidUnique(String uid) throws LDAPSearchException {
    if (isUidUnique(uid)) {
      throw new RuntimeException("Already in use as user name");
    }
  }

  public void checkPosixGidUnique(String gid) throws LDAPSearchException {
    if (isGidUnique(gid)) {
      throw new RuntimeException("Already in use as group name");
    }
  }

  public boolean isGidUnique(String gid) throws LDAPSearchException {
    var res =
      ldap.search(
        ManagerLDAP.GROUPS_BASE,
        SUB,
        Filter.createANDFilter(
          Filter.createEqualityFilter("objectClass", "groupOfEntries"),
          Filter.createEqualityFilter("objectClass", "posixGroup"),
          Filter.createEqualityFilter("cn", gid)));
    return res.getEntryCount() > 0;
  }

  public void addSshKey(String uid, String key) throws LDAPException {
    Entry userBefore = getUserEntry(uid);
    Entry userAfter = userBefore.duplicate();
    userAfter.addAttribute("sshPublicKey", key);
    var mods = Entry.diff(userBefore, userAfter, true, true);
    if (mods.size() > 0) {
      ldap.modify(new ModifyRequest(userBefore.getDN(), mods));
    }
  }

  public void removeSshKey(String uid, String key) throws LDAPException {
    Entry userBefore = getUserEntry(uid);
    Entry userAfter = userBefore.duplicate();
    userAfter.removeAttributeValue("sshPublicKey", key);
    var mods = Entry.diff(userBefore, userAfter, true, true);
    if (mods.size() > 0) {
      ldap.modify(new ModifyRequest(userBefore.getDN(), mods));
    }
  }
}
