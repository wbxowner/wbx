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

package wbx.http;

import wbx.Account;
import wbx.Attachment;
import wbx.Currency;
import wbx.WbxException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Claim currency units and receive back WBX invested into this currency before it became active
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>units - the number of currency units claimed<br>
 * This value is multiplied by current currency rate and the result is added to the sender WBX account balance.
 * </ul>
 * <p>
 * Constraints
 * <p>This transaction is allowed only when the currency is {@link wbx.CurrencyType#CLAIMABLE} and is already active.<br>
 */
public final class CurrencyReserveClaim extends CreateTransaction {

    static final CurrencyReserveClaim instance = new CurrencyReserveClaim();

    private CurrencyReserveClaim() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "units");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws WbxException {
        Currency currency = ParameterParser.getCurrency(req);
        long units = ParameterParser.getLong(req, "units", 0, currency.getReserveSupply(), false);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemReserveClaim(currency.getId(), units);
        return createTransaction(req, account, attachment);

    }

}
