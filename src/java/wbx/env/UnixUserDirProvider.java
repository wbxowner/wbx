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

package wbx.env;

import java.nio.file.Paths;

public class UnixUserDirProvider extends DesktopUserDirProvider {

    private static final String WIN_USER_HOME = Paths.get(System.getProperty("user.home"), ".wbx").toString();

    @Override
    public String getUserHomeDir() {
        return WIN_USER_HOME;
    }
}
