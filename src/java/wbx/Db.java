/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package wbx;

import wbx.db.BasicDb;
import wbx.db.TransactionalDb;

public final class Db {

    public static final String PREFIX = Constants.isTestnet ? "wbx.testDb" : "wbx.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Wbx.getIntProperty("wbx.dbCacheKB"))
            .dbUrl(Wbx.getStringProperty(PREFIX + "Url"))
            .dbType(Wbx.getStringProperty(PREFIX + "Type"))
            .dbDir(Wbx.getStringProperty(PREFIX + "Dir"))
            .dbParams(Wbx.getStringProperty(PREFIX + "Params"))
            .dbUsername(Wbx.getStringProperty(PREFIX + "Username"))
            .dbPassword(Wbx.getStringProperty(PREFIX + "Password", null, true))
            .maxConnections(Wbx.getIntProperty("wbx.maxDbConnections"))
            .loginTimeout(Wbx.getIntProperty("wbx.dbLoginTimeout"))
            .defaultLockTimeout(Wbx.getIntProperty("wbx.dbDefaultLockTimeout") * 1000)
            .maxMemoryRows(Wbx.getIntProperty("wbx.dbMaxMemoryRows"))
    );

    static void init() {
        db.init(new WinDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

}
