/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
  NRS.custom = {};
  
  // called from nrs.login.js after all other page's setup functions
  NRS.custom.setup = function() {
    NRS.custom.setupSidebarMenuItems();
  };
  
  NRS.custom.setupSidebarMenuItems = function() {
    console.log('made it!');
  
    // add items to messages
    /*NRS.appendMenuItemToTSMenuItem('sidebar_messages', {
      "titleHTML": '<span data-i18n="send_wbx">Send WBX</span>',
      "type": 'MODAL',
      "modalId": 'send_money_modal'
    });*/
    NRS.appendMenuItemToTSMenuItem('sidebar_messages', {
      "titleHTML": '<span data-i18n="send_message">Send Message</span>',
      "type": 'MODAL',
      "modalId": 'send_message_modal'
    });
    
    // sidebar settings menu
    NRS.addTreeviewSidebarMenuItem({
			"id": 'header_settings',
			"titleHTML": '<span data-i18n="other">Other</span>',
			"page": 'my_messages',
			"desiredPosition": 1000
		});
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="exchange">Deposit Coin</span>',
      "type": 'PAGE',
      "page": 'exchange'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="contacts">Contacts</span>',
      "type": 'PAGE',
      "page": 'contacts'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="blocks">Blockchain Explorer</span>',
      "type": 'PAGE',
      "page": 'blocks'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="peers">Blockchain nodes</span>',
      "type": 'PAGE',
      "page": 'peers'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="generators">PoS miners</span>',
      "type": 'PAGE',
      "page": 'generators'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="monitors">Monitors</span></a>',
      "type": 'PAGE',
      "page": 'funding_monitors'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="account_settings">Account Settings</span>',
      "type": 'PAGE',
      "page": 'settings'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="generate_token">Generate Token</span>',
      "type": 'MODAL',
      "modalId": 'token_modal'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="calculate_hash">Calculate Hash</span>',
      "type": 'MODAL',
      "modalId": 'hash_modal'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="transaction_operations">Transaction Operations</span>',
      "type": 'MODAL',
      "modalId": 'transaction_json_modal'
    });
    /*NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="win_forum">WBX Forum</span>',
      "type": 'LINK',
      "href": 'http://forum.winbit.com/'
    });*/
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="logout">Log Out</span>',
      "type": 'ACTION',
      "action": 'NRS.logout()'
    });
    NRS.appendMenuItemToTSMenuItem('header_settings', {
      "titleHTML": '<span data-i18n="logout_clear_user_data">Log Out and Clear User Data</a></span>',
      "type": 'MODAL',
      "modalId": 'logout_clear_user_data_modal'
    });
    
  };

	return NRS;
}(NRS || {}, jQuery));