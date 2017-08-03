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

import wbx.WbxException;
import wbx.TaggedData;
import wbx.db.DbIterator;
import wbx.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchTaggedData extends APIServlet.APIRequestHandler {

    static final SearchTaggedData instance = new SearchTaggedData();

    private SearchTaggedData() {
        super(new APITag[] {APITag.DATA, APITag.SEARCH}, "query", "tag", "channel", "account", "firstIndex", "lastIndex", "includeData");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws WbxException {
        long accountId = ParameterParser.getAccountId(req, "account", false);
        String query = ParameterParser.getSearchQuery(req);
        String channel = Convert.emptyToNull(req.getParameter("channel"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeData = "true".equalsIgnoreCase(req.getParameter("includeData"));

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("data", jsonArray);
        try (DbIterator<TaggedData> data = TaggedData.searchData(query, channel, accountId, firstIndex, lastIndex)) {
            while (data.hasNext()) {
                jsonArray.add(JSONData.taggedData(data.next(), includeData));
            }
        }

        return response;
    }

}
