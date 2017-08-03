#!/bin/bash

#sudo chmod a+x /home/wbx/update_server_param.sh

#clear
#service wbx-server stop > /dev/null 2>&1 &
echo "Stop WBX server"

#/etc/init.d/wbx-server stop > /dev/null 2>&1 &
/etc/init.d/wbx-server stop
#echo "wait 5 secs..."
#sleep 5
#cd /home/wbx
#rm -rf wbx_db

#ignorePeerAnnouncedAddress=false
ignorePeerAnnouncedAddress=false
usePeersDb=true
savePeers=true

myAddress=`ifconfig eth0 2>/dev/null|awk '/inet / {print $2}'|sed 's/addr://'`;
echo 'myAddress='$myAddress
#myInternalAddress=`ifconfig eth0:1 2>/dev/null|awk '/inet / {print $2}'|sed 's/addr://'`;
#echo 'myInternalAddress='$myInternalAddress

myPlatformDefault='Node-'$myAddress
homeDirDefault=/home/wbx
reductorFeeDefault=400

#echo "Type the home dir ($homeDirDefault):"
#read homeDir
if [ -z "$homeDir" ]; then
	homeDir=$homeDirDefault
fi
echo "Home dir is $homeDir"
#echo "Type the platform name to be announced to peers ($myPlatformDefault):"
#read myPlatform
#if [ -z "$myPlatform" ]; then
#	myPlatform=$myPlatformDefault
#fi
#echo "Type the value of reductorFee ($reductorFeeDefault):"
#read reductorFee
if [ -z "$reductorFee" ]; then
	reductorFee=$reductorFeeDefault
fi
#echo "Type the server HALLMARK:"
#read myHallmark
if [ -z "$reductorFee" ]; then
	reductorFee=1
fi
echo "reductorFee is $reductorFee"
#echo "Type the forger secret phrase:"
#read forgerSecretPhrase
#forgerSecretPhrase=$(sed 's/[\/&]/\\&/g' <<<"$forgerSecretPhrase")
#echo $forgerSecretPhrase

case $myAddress in
    172.104.58.103|172.104.45.27|172.104.53.22|139.162.19.222|172.104.36.203)
          PEERNODE=(172.104.58.103 172.104.45.27 172.104.53.22 139.162.19.222 172.104.36.203)
          #PEERNODE=(192.168.143.78 192.168.138.152 192.168.148.89 192.168.159.24 172.104.36.203)
          #PEERNODE=(192.168.143.78 192.168.138.152 172.104.53.22 139.162.19.222 172.104.36.203)
          ;;
     *)
          echo 'Undefined myAddress '$myAdress
          exit
          ;;
esac

file=conf/wbx-default.properties.base
file_new=conf/wbx-default.properties
peerServerPort=27874
apiServerPort=27876
apiServerSSLPort=27876
adminPassword=tantoVaLagatta$myAddress
currencyName=WBX
uiServerPort=27875
disabledAPITags='Aliases;Digital Goods Store;Monetary System;Debug;Add-ons;'
#ACCOUNTS("Accounts"), ACCOUNT_CONTROL("Account Control"), ALIASES("Aliases"), AE("Asset Exchange"), BLOCKS("Blocks"),
#CREATE_TRANSACTION("Create Transaction"), DGS("Digital Goods Store"), FORGING("Forging"), MESSAGES("Messages"),
#MS("Monetary System"), NETWORK("Networking"), PHASING("Phasing"), SEARCH("Search"), INFO("Server Info"),
#SHUFFLING("Shuffling"), DATA("Tagged Data"), TOKENS("Tokens"), TRANSACTIONS("Transactions"), VS("Voting System"),
#UTILS("Utils"), DEBUG("Debug"), ADDONS("Add-ons");

for PEER in "${PEERNODE[@]}"
do
	if [ $PEER != $myAddress ] ; then
		wellKnownPeers=$wellKnownPeers$PEER'; '
	fi
done
for PEER in "${PEERNODEBLACKLIST[@]}"
do
	if [ $PEER != $myAddress ] ; then
		knownBlacklistedPeers=$knownBlacklistedPeers$PEER'; '
	fi
done

enableApiTestUI=false
enableAPIServer=true
maxPrunableLifetime=7776000
case $myAddress in
     172.104.58.103)
          forgerSecretPhrase='a195ff71da37c88155089dd1ba45ea709244076bf8248a4e13e99e0900513a5b0b783b1fe17b18b7d38c61060258150c2fd46f9da9bd988ec9bf8218d8a61b8c'
          myHallmark=''
          #myDomain=$myAddress
          myDomain=192.168.143.78
          maxPrunableLifetime=-1
          ;;
     172.104.45.27)
          forgerSecretPhrase='9191a5d351aebde9488c2c5cdd11771560d8cd633ee468bec61a977847c73aaf61a917bab3eaf7a0b4ba3b75b7035e9803f1d9566221212235f54a92bf8d8702'
          myHallmark=''
          #myDomain=$myAddress
          myDomain=192.168.138.152
          ;;
     172.104.53.22)
          forgerSecretPhrase='0b08c8e10a11751da8d474f2d59b99ddb251ded7751ccd063b850a5df08b39aa82ddcddcc4200711fe474ea33dd42f75cc9d3368f7c0835b56755d483ebd72b8'
          myHallmark=''
          myDomain=$myAddress
          #myDomain=192.168.148.89
          maxPrunableLifetime=-1
          ;;
     139.162.19.222)
          forgerSecretPhrase='53514ddf0424c8e2509031c211e5387cdca71fc2890d2b84e2acc386b4392b17ebd5e007bd34b2cc962ac21cc6f5abaefd3355abb0d1563a3e522910e7980f77'
          myHallmark=''
          myDomain=$myAddress
          #myDomain=192.168.159.24
          ;;
     172.104.36.203)
          forgerSecretPhrase='cadef653c8c5fd22c6c71c53085ab14d2523a532a498f21692dce75abcbcaff543e42038b6bcca5d37b6717e3119097ec8f4ab9ff738112d6127f0007a806960'
          myHallmark=''
          #enableAPIServer=false
          myDomain=$myAddress
          ;;
     *)
          forgerSecretPhrase=''
          myHallmark=''
          myDomain=$myAddress
          ;;
esac
myDomain=$myAddress

myPlatform='Node-'$myDomain
echo "myDomain is $myDomain"
echo "myPlatform is $myPlatform"
echo "wellKnownPeers is $wellKnownPeers"

forgerSecretPhrase=$(sed 's/[\/&]/\\&/g' <<<"$forgerSecretPhrase")
echo 'Found forgerSecretPhrase='$forgerSecretPhrase

if [[ -f $homeDir/$file ]] && [[ -w $homeDir/$file ]]; then
	rm -rf $homeDir/$file_new
	rm -rf $homeDir/wbx.properties
	cp $homeDir/$file $homeDir/$file_new
	sed -i -- "s/wbx.myAddress=/wbx.myAddress=$myDomain/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.myPlatform=/wbx.myPlatform=$myPlatform/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.myHallmark=/wbx.myHallmark=$myHallmark/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.wellKnownPeers=/wbx.wellKnownPeers=$wellKnownPeers/g" "$homeDir/$file_new"
	#sed -i -- "s/wbx.defaultPeers=/wbx.defaultPeers=$wellKnownPeers/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.knownBlacklistedPeers=/wbx.knownBlacklistedPeers=$knownBlacklistedPeers/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.forgerSecretPhrase=/#wbx.forgerSecretPhrase=$forgerSecretPhrase/g" "$homeDir/$file_new"
	#echo $forgerSecretPhrase   sed -i -- "s/wbx.forgerSecretPhrase=/wbx.forgerSecretPhrase=\1/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.reductorFee=/wbx.reductorFee=$reductorFee/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.peerServerPort=/wbx.peerServerPort=$peerServerPort/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.apiServerPort=/wbx.apiServerPort=$apiServerPort/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.apiServerSSLPort=/wbx.apiServerSSLPort=$apiServerSSLPort/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.adminPassword=/wbx.adminPassword=$adminPassword/g" "$homeDir/$file_new"
	#sed -i -- "s/wbx.currencyName=/wbx.currencyName=$currencyName/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.uiServerPort=/wbx.uiServerPort=$uiServerPort/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.disabledAPITags=/wbx.disabledAPITags=$disabledAPITags/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.maxPrunableLifetime=/wbx.maxPrunableLifetime=$maxPrunableLifetime/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.enableApiTestUI=/wbx.enableApiTestUI=$enableApiTestUI/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.enableAPIServer=/wbx.enableAPIServer=$enableAPIServer/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.ignorePeerAnnouncedAddress=/wbx.ignorePeerAnnouncedAddress=$ignorePeerAnnouncedAddress/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.usePeersDb=/wbx.usePeersDb=$usePeersDb/g" "$homeDir/$file_new"
	sed -i -- "s/wbx.savePeers=/wbx.savePeers=$savePeers/g" "$homeDir/$file_new"

fi

#######################################################
#               ONLY FIRST TIME                       #
#echo 'rm -rf /home/wbx/wbx_db_old'
#rm -rf /home/wbx/wbx_db_old
#echo 'mv /home/wbx/wbx_db /home/wbx/wbx_db_old'
#mv /home/wbx/wbx_db /home/wbx/wbx_db_old
#######################################################
echo 'rm -rf  $homeDir/src/java/windesktop.old'
rm -rf  $homeDir//src/java/windesktop.old
echo 'mv  $homeDir/src/java/windesktop  $homeDir/src/java/windesktop.old'
mv  $homeDir/src/java/windesktop  $homeDir/src/java/windesktop.old
echo 'rm -rf  $homeDir/jre-old'
rm -rf  $homeDir/jre-old
echo 'mv  $homeDir/jre  $homeDir/jre-old'
mv  $homeDir/jre  $homeDir/jre-old

#case $myAddress in
#     172.104.36.203)
#			echo 'remove html/www e wtml/nrs folder'
#			rm -rf  $homeDir/html/www
#			rm -rf  $homeDir/html/nrs
#          ;;
#     *)
#          echo 'No delete required'
#          ;;
#esac

echo "Recompile WBX server"
#/home/wbx/compile.sh > /dev/null
#/home/wbx/compile.sh > /dev/null 2>&1 &
$homeDir/compile.sh
#echo "wait almost 30 secs..."
#sleep 30
#echo "Finished compiling"
echo "Restart WBX server"
#######################################################
#          ONLY FIRST TIME DOUBLE RESTART             #
#/etc/init.d/wbx-server restart > /dev/null 2>&1
#######################################################
/etc/init.d/wbx-server restart > /dev/null 2>&1 &
#service wbx-server restart > /dev/null 2>&1 &
echo "End script"

