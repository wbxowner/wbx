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

import wbx.crypto.Crypto;
import wbx.db.DbIterator;
import wbx.db.DerivedDbTable;
import wbx.db.FilteringIterator;
import wbx.db.FullTextTrigger;
import wbx.peer.Peer;
import wbx.peer.Peers;
import wbx.util.Convert;
import wbx.util.JSON;
import wbx.util.Listener;
import wbx.util.Listeners;
import wbx.util.Logger;
import wbx.util.ThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

final class BlockchainProcessorImpl implements BlockchainProcessor {

    private static final byte[] CHECKSUM_TRANSPARENT_FORGING =
            new byte[] {
                    -122, -111, -35, 76, 59, 79, -75, 117, 34, 2, -70, -65, -38, 59, 0, 57,
                    120, 0, -107, 11, 97, -48, 21, 36, 48, -94, 88, 54, -14, 60, -101, -80
            };
    private static final byte[] CHECKSUM_NQT_BLOCK = Constants.isTestnet ?
            new byte[] {
                    110, -1, -56, -56, -58, 48, 43, 12, -41, -37, 90, -93, 80, 20, 3, -76, -84,
                    -15, -113, -34, 30, 32, 57, 85, -30, 16, -10, 127, -101, 17, 121, 124
            }
            :
            new byte[] {
                    -90, -42, -57, -76, 88, -49, 127, 6, -47, -72, -39, -56, 51, 90, -90, -105,
                    121, 71, -94, -97, 49, -24, -12, 86, 7, -48, 90, -91, -24, -105, -17, -104
            };
    private static final byte[] CHECKSUM_MONETARY_SYSTEM_BLOCK = Constants.isTestnet ?
            new byte[] {
                    119, 51, 105, -101, -74, -49, -49, 19, 11, 103, -84, 80, -46, -5, 51, 42,
                    84, 88, 87, -115, -19, 104, 49, -93, -41, 84, -34, -92, 103, -48, 29, 44
            }
            :
            new byte[] {
                    -117, -101, 74, 111, -114, 39, 80, -67, 48, 86, 68, 106, -105, 2, 84, -109,
                    1, 4, -20, -82, -112, -112, 25, 119, 23, -113, 126, -121, -36, 15, -32, -24
            };
    private static final byte[] CHECKSUM_PHASING_BLOCK = Constants.isTestnet ?
            new byte[] {
                    4, -100, -26, 47, 93, 1, -114, 86, -42, 46, -103, 13, 120, 0, 2, 100, -52,
                    -67, 109, -90, 87, 13, 30, -110, -58, -70, -94, 21, 105, -58, 20, 0
            }
            :
            new byte[] {
                    -88, -128, 68, -118, 10, -62, 110, 19, -73, 61, 34, -76, 35, 73, -101, 9,
                    33, -111, 40, 114, 27, 105, 54, 0, 16, -97, 115, -12, -110, -88, 1, -15
            };
    private static final byte[] CHECKSUM_16 = Constants.isTestnet ?
            new byte[] {
                    -12, 21, 56, 106, -58, -126, 123, 33, 117, 11, -79, 28, -79, -45, 7, 69,
                    120, 71, -3, 27, 67, -85, 30, -25, -12, 127, 76, -60, -114, 41, -46, 55
            }
            :
            new byte[] {
                    4, -96, 70, -17, 32, 17, 76, -92, 127, -127, 76, -77, 38, 7, 36, -113, 69,
                    26, -91, -94, -81, -70, 62, 30, 114, 63, -102, -55, -75, 25, -17, -12
            };
    private static final byte[] CHECKSUM_17 = Constants.isTestnet ?
            new byte[] {
                    -19, -44, -49, 101, 5, -57, 51, 119, 16, 36, -3, 123, 90, -83, 89, 55, 72,
                    116, 4, 27, -14, 114, 28, 79, -104, 100, -74, 61, -64, -6, -53, 103
            }
            :
            new byte[] {
                    90, 15, -6, -42, -105, -103, 83, -17, -112, 51, -53, 110, 98, -54, -4, 2,
                    30, -69, 25, 91, 52, 126, -40, -91, -23, 118, -121, 70, 116, 60, -49, -86
            };
    private static final byte[] CHECKSUM_18 = Constants.isTestnet ?
            new byte[] {
                    98, 53, 16, 32, 124, -49, 117, -11, 50, -122, 110, 5, -47, -11, -36, -48,
                    -12, 10, -68, -105, 125, -61, -61, -62, -98, -64, -20, -110, 96, 20, 116, -52
            }
            :
            new byte[] {
                    28, -67, 28, 87, -21, 91, -74, 115, -37, 67, 74, -32, -92, 53, -58, 62,
                    -60, 54, 58, -94, 9, 5, 26, -103, -19, 47, 78, 117, -49, 42, -14, 109
            };
    private static final byte[] CHECKSUM_19 = Constants.isTestnet ?
            new byte[] {
                    83, -5, -8, 51, 67, 28, 11, 101, -77, -57, 98, -113, 76, 2, -5, -97, -102,
                    112, -51, -128, 79, -66, -81, -76, 113, 14, 51, 117, -77, -98, 84, 104
            }
            :
            new byte[] {
                    -19, -60, 82, 93, 111, 77, 127, 100, 77, 102, 29, 104, 39, 78, -123, -108,
                    -25, -42, 59, 22, -9, -110, -32, -126, -18, 31, -46, 102, -75, -113, 6, 104
            };
    private static final byte[] CHECKSUM_20 = Constants.isTestnet ?
            new byte[] {
                    33, -16, 85, -127, -102, 97, 22, -52, -13, 24, 102, -53, -106, 35, 20, 33,
                    78, 37, -43, 63, 21, -59, -20, 120, 5, 80, 93, 71, -98, -58, 78, 95
            }
            :
            new byte[] {
                    -31, 16, 18, -38, -86, 3, -111, -9, 3, -32, 87, 8, 70, 35, -33, -56, 91,
                    -72, -55, -96, -120, -127, -116, 2, -21, -89, -7, 56, -114, -66, 72, -49
            };
    private static final byte[] CHECKSUM_21 = Constants.isTestnet ?
            new byte[] {
                    -63, -51, -56, -76, 13, 21, -47, 69, -108, -28, -124, 108, -17, 27, 30, -65,
                    -95, 110, -9, 35, 59, 112, -20, 122, -44, -86, -54, 51, 46, 80, -13, -26
            }
            :
            new byte[] {
                    -26, -121, -115, -116, -62, -120, -99, -74, -52, -39, 9, 52, 20, 92, -42, 115,
                    19, -67, 7, 51, 4, -100, -41, 41, 57, -102, 19, -128, -109, -52, -68, -15
            };


    private static final BlockchainProcessorImpl instance = new BlockchainProcessorImpl();

    static BlockchainProcessorImpl getInstance() {
        return instance;
    }

    private final BlockchainImpl blockchain = BlockchainImpl.getInstance();

    private final ExecutorService networkService = Executors.newCachedThreadPool();
    private final List<DerivedDbTable> derivedTables = new CopyOnWriteArrayList<>();
    private final boolean trimDerivedTables = Wbx.getBooleanProperty("wbx.trimDerivedTables");
    private final int defaultNumberOfForkConfirmations = Wbx.getIntProperty(Constants.isTestnet
            ? "wbx.testnetNumberOfForkConfirmations" : "wbx.numberOfForkConfirmations");
    private final boolean simulateEndlessDownload = Wbx.getBooleanProperty("wbx.simulateEndlessDownload");

    private int initialScanHeight;
    private volatile int lastTrimHeight;
    private volatile int lastRestoreTime = 0;
    private final Set<Long> prunableTransactions = new HashSet<>();

    private final Listeners<Block, Event> blockListeners = new Listeners<>();
    private volatile Peer lastBlockchainFeeder;
    private volatile int lastBlockchainFeederHeight;
    private volatile boolean getMoreBlocks = true;

    private volatile boolean isTrimming;
    private volatile boolean isScanning;
    private volatile boolean isDownloading;
    private volatile boolean isProcessingBlock;
    private volatile boolean isRestoring;
    private volatile boolean alreadyInitialized = false;

    private final Runnable getMoreBlocksThread = new Runnable() {

        private final JSONStreamAware getCumulativeDifficultyRequest;

        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getCumulativeDifficulty");
            getCumulativeDifficultyRequest = JSON.prepareRequest(request);
        }

        private boolean peerHasMore;
        private List<Peer> connectedPublicPeers;
        private List<Long> chainBlockIds;
        private long totalTime = 1;
        private int totalBlocks;

        @Override
        public void run() {
            try {
                //
                // Download blocks until we are up-to-date
                //
                while (true) {
                    if (!getMoreBlocks) {
                        return;
                    }
                    int chainHeight = blockchain.getHeight();
                    downloadPeer();
                    if (blockchain.getHeight() == chainHeight) {
                        if (isDownloading && !simulateEndlessDownload) {
                            Logger.logMessage("Finished blockchain download");
                            isDownloading = false;
                        }
                        break;
                    }
                }
                //
                // Restore prunable data
                //
                int now = Wbx.getEpochTime();
                if (!isRestoring && !prunableTransactions.isEmpty() && now - lastRestoreTime > 60 * 60) {
                    isRestoring = true;
                    lastRestoreTime = now;
                    networkService.submit(new RestorePrunableDataTask());
                }
            } catch (InterruptedException e) {
                Logger.logDebugMessage("Blockchain download thread interrupted");
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
                System.exit(1);
            }
        }

        private void downloadPeer() throws InterruptedException {
            try {
                long startTime = System.currentTimeMillis();
                int numberOfForkConfirmations = blockchain.getHeight() > Constants.LAST_CHECKSUM_BLOCK - 720 ?
                        defaultNumberOfForkConfirmations : Math.min(1, defaultNumberOfForkConfirmations);
                connectedPublicPeers = Peers.getPublicPeers(Peer.State.CONNECTED, true);
                if (connectedPublicPeers.size() <= numberOfForkConfirmations) {
                    return;
                }
                peerHasMore = true;
                final Peer peer = Peers.getWeightedPeer(connectedPublicPeers);
                if (peer == null) {
                    return;
                }
                JSONObject response = peer.send(getCumulativeDifficultyRequest);
                if (response == null) {
                    return;
                }
                BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
                String peerCumulativeDifficulty = (String) response.get("cumulativeDifficulty");
                if (peerCumulativeDifficulty == null) {
                    return;
                }
                BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
                if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
                    return;
                }
                if (response.get("blockchainHeight") != null) {
                    lastBlockchainFeeder = peer;
                    lastBlockchainFeederHeight = ((Long) response.get("blockchainHeight")).intValue();
                }
                if (betterCumulativeDifficulty.equals(curCumulativeDifficulty)) {
                    return;
                }

                long commonMilestoneBlockId = Genesis.GENESIS_BLOCK_ID;

                if (blockchain.getLastBlock().getId() != Genesis.GENESIS_BLOCK_ID) {
                    commonMilestoneBlockId = getCommonMilestoneBlockId(peer);
                }
                if (commonMilestoneBlockId == 0 || !peerHasMore) {
                    return;
                }

                chainBlockIds = getBlockIdsAfterCommon(peer, commonMilestoneBlockId, false);
                if (chainBlockIds.size() < 2 || !peerHasMore) {
                    return;
                }

                final long commonBlockId = chainBlockIds.get(0);
                final Block commonBlock = blockchain.getBlock(commonBlockId);
                if (commonBlock == null || blockchain.getHeight() - commonBlock.getHeight() >= 720) {
                    return;
                }
                if (simulateEndlessDownload) {
                    isDownloading = true;
                    return;
                }
                if (!isDownloading && lastBlockchainFeederHeight - commonBlock.getHeight() > 10) {
                    Logger.logMessage("Blockchain download in progress");
                    isDownloading = true;
                }

                blockchain.updateLock();
                try {
                    if (betterCumulativeDifficulty.compareTo(blockchain.getLastBlock().getCumulativeDifficulty()) <= 0) {
                        return;
                    }
                    long lastBlockId = blockchain.getLastBlock().getId();
                    downloadBlockchain(peer, commonBlock, commonBlock.getHeight());
                    if (blockchain.getHeight() - commonBlock.getHeight() <= 10) {
                        return;
                    }

                    int confirmations = 0;
                    for (Peer otherPeer : connectedPublicPeers) {
                        if (confirmations >= numberOfForkConfirmations) {
                            break;
                        }
                        if (peer.getHost().equals(otherPeer.getHost())) {
                            continue;
                        }
                        chainBlockIds = getBlockIdsAfterCommon(otherPeer, commonBlockId, true);
                        if (chainBlockIds.isEmpty()) {
                            continue;
                        }
                        long otherPeerCommonBlockId = chainBlockIds.get(0);
                        if (otherPeerCommonBlockId == blockchain.getLastBlock().getId()) {
                            confirmations++;
                            continue;
                        }
                        Block otherPeerCommonBlock = blockchain.getBlock(otherPeerCommonBlockId);
                        if (blockchain.getHeight() - otherPeerCommonBlock.getHeight() >= 720) {
                            continue;
                        }
                        String otherPeerCumulativeDifficulty;
                        JSONObject otherPeerResponse = peer.send(getCumulativeDifficultyRequest);
                        if (otherPeerResponse == null || (otherPeerCumulativeDifficulty = (String) response.get("cumulativeDifficulty")) == null) {
                            continue;
                        }
                        if (new BigInteger(otherPeerCumulativeDifficulty).compareTo(blockchain.getLastBlock().getCumulativeDifficulty()) <= 0) {
                            continue;
                        }
                        Logger.logDebugMessage("Found a peer with better difficulty");
                        downloadBlockchain(otherPeer, otherPeerCommonBlock, commonBlock.getHeight());
                    }
                    Logger.logDebugMessage("Got " + confirmations + " confirmations");

                    if (blockchain.getLastBlock().getId() != lastBlockId) {
                        long time = System.currentTimeMillis() - startTime;
                        totalTime += time;
                        int numBlocks = blockchain.getHeight() - commonBlock.getHeight();
                        totalBlocks += numBlocks;
                        Logger.logMessage("Downloaded " + numBlocks + " blocks in "
                                + time / 1000 + " s, " + (totalBlocks * 1000) / totalTime + " per s, "
                                + totalTime * (lastBlockchainFeederHeight - blockchain.getHeight()) / ((long) totalBlocks * 1000 * 60) + " min left");
                    } else {
                        Logger.logDebugMessage("Did not accept peer's blocks, back to our own fork");
                    }
                } finally {
                    blockchain.updateUnlock();
                }

            } catch (WbxException.StopException e) {
                Logger.logMessage("Blockchain download stopped: " + e.getMessage());
                throw new InterruptedException("Blockchain download stopped");
            } catch (Exception e) {
                Logger.logMessage("Error in blockchain download thread", e);
            }
        }

        private long getCommonMilestoneBlockId(Peer peer) {

            String lastMilestoneBlockId = null;

            while (true) {
                JSONObject milestoneBlockIdsRequest = new JSONObject();
                milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
                if (lastMilestoneBlockId == null) {
                    milestoneBlockIdsRequest.put("lastBlockId", blockchain.getLastBlock().getStringId());
                } else {
                    milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
                }

                JSONObject response = peer.send(JSON.prepareRequest(milestoneBlockIdsRequest));
                if (response == null) {
                    return 0;
                }
                JSONArray milestoneBlockIds = (JSONArray) response.get("milestoneBlockIds");
                if (milestoneBlockIds == null) {
                    return 0;
                }
                if (milestoneBlockIds.isEmpty()) {
                    return Genesis.GENESIS_BLOCK_ID;
                }
                // prevent overloading with blockIds
                if (milestoneBlockIds.size() > 20) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getHost() + " sends too many milestoneBlockIds, blacklisting");
                    peer.blacklist("Too many milestoneBlockIds");
                    return 0;
                }
                if (Boolean.TRUE.equals(response.get("last"))) {
                    peerHasMore = false;
                }
                for (Object milestoneBlockId : milestoneBlockIds) {
                    long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                    if (BlockDb.hasBlock(blockId)) {
                        if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
                            peerHasMore = false;
                        }
                        return blockId;
                    }
                    lastMilestoneBlockId = (String) milestoneBlockId;
                }
            }

        }

        private List<Long> getBlockIdsAfterCommon(final Peer peer, final long startBlockId, final boolean countFromStart) {
            long matchId = startBlockId;
            List<Long> blockList = new ArrayList<>(720);
            boolean matched = false;
            int limit = countFromStart ? 720 : 1440;
            while (true) {
                JSONObject request = new JSONObject();
                request.put("requestType", "getNextBlockIds");
                request.put("blockId", Long.toUnsignedString(matchId));
                request.put("limit", limit);
                JSONObject response = peer.send(JSON.prepareRequest(request));
                if (response == null) {
                    return Collections.emptyList();
                }
                JSONArray nextBlockIds = (JSONArray) response.get("nextBlockIds");
                if (nextBlockIds == null || nextBlockIds.size() == 0) {
                    break;
                }
                // prevent overloading with blockIds
                if (nextBlockIds.size() > limit) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getHost() + " sends too many nextBlockIds, blacklisting");
                    peer.blacklist("Too many nextBlockIds");
                    return Collections.emptyList();
                }
                boolean matching = true;
                int count = 0;
                for (Object nextBlockId : nextBlockIds) {
                    long blockId = Convert.parseUnsignedLong((String)nextBlockId);
                    if (matching) {
                        if (BlockDb.hasBlock(blockId)) {
                            matchId = blockId;
                            matched = true;
                        } else {
                            blockList.add(matchId);
                            blockList.add(blockId);
                            matching = false;
                        }
                    } else {
                        blockList.add(blockId);
                        if (blockList.size() >= 720) {
                            break;
                        }
                    }
                    if (countFromStart && ++count >= 720) {
                        break;
                    }
                }
                if (!matching || countFromStart) {
                    break;
                }
            }
            if (blockList.isEmpty() && matched) {
                blockList.add(matchId);
            }
            return blockList;
        }

        /**
         * Download the block chain
         *
         * @param   feederPeer              Peer supplying the blocks list
         * @param   commonBlock             Common block
         * @throws  InterruptedException    Download interrupted
         */
        private void downloadBlockchain(final Peer feederPeer, final Block commonBlock, final int startHeight) throws InterruptedException {
            Map<Long, PeerBlock> blockMap = new HashMap<>();
            //
            // Break the download into multiple segments.  The first block in each segment
            // is the common block for that segment.
            //
            List<GetNextBlocks> getList = new ArrayList<>();
            int segSize = 36;
            int stop = chainBlockIds.size() - 1;
            for (int start = 0; start < stop; start += segSize) {
                getList.add(new GetNextBlocks(chainBlockIds, start, Math.min(start + segSize, stop)));
            }
            int nextPeerIndex = ThreadLocalRandom.current().nextInt(connectedPublicPeers.size());
            long maxResponseTime = 0;
            Peer slowestPeer = null;
            //
            // Issue the getNextBlocks requests and get the results.  We will repeat
            // a request if the peer didn't respond or returned a partial block list.
            // The download will be aborted if we are unable to get a segment after
            // retrying with different peers.
            //
            download: while (!getList.isEmpty()) {
                //
                // Submit threads to issue 'getNextBlocks' requests.  The first segment
                // will always be sent to the feeder peer.  Subsequent segments will
                // be sent to the feeder peer if we failed trying to download the blocks
                // from another peer.  We will stop the download and process any pending
                // blocks if we are unable to download a segment from the feeder peer.
                //
                for (GetNextBlocks nextBlocks : getList) {
                    Peer peer;
                    if (nextBlocks.getRequestCount() > 1) {
                        break download;
                    }
                    if (nextBlocks.getStart() == 0 || nextBlocks.getRequestCount() != 0) {
                        peer = feederPeer;
                    } else {
                        if (nextPeerIndex >= connectedPublicPeers.size()) {
                            nextPeerIndex = 0;
                        }
                        peer = connectedPublicPeers.get(nextPeerIndex++);
                    }
                    if (nextBlocks.getPeer() == peer) {
                        break download;
                    }
                    nextBlocks.setPeer(peer);
                    Future<List<BlockImpl>> future = networkService.submit(nextBlocks);
                    nextBlocks.setFuture(future);
                }
                //
                // Get the results.  A peer is on a different fork if a returned
                // block is not in the block identifier list.
                //
                Iterator<GetNextBlocks> it = getList.iterator();
                while (it.hasNext()) {
                    GetNextBlocks nextBlocks = it.next();
                    List<BlockImpl> blockList;
                    try {
                        blockList = nextBlocks.getFuture().get();
                    } catch (ExecutionException exc) {
                        throw new RuntimeException(exc.getMessage(), exc);
                    }
                    if (blockList == null) {
                        nextBlocks.getPeer().deactivate();
                        continue;
                    }
                    Peer peer = nextBlocks.getPeer();
                    int index = nextBlocks.getStart() + 1;
                    for (BlockImpl block : blockList) {
                        if (block.getId() != chainBlockIds.get(index)) {
                            break;
                        }
                        blockMap.put(block.getId(), new PeerBlock(peer, block));
                        index++;
                    }
                    if (index > nextBlocks.getStop()) {
                        it.remove();
                    } else {
                        nextBlocks.setStart(index - 1);
                    }
                    if (nextBlocks.getResponseTime() > maxResponseTime) {
                        maxResponseTime = nextBlocks.getResponseTime();
                        slowestPeer = nextBlocks.getPeer();
                    }
                }

            }
            if (slowestPeer != null && connectedPublicPeers.size() >= Peers.maxNumberOfConnectedPublicPeers && chainBlockIds.size() > 360) {
                Logger.logDebugMessage(slowestPeer.getHost() + " took " + maxResponseTime + " ms, disconnecting");
                slowestPeer.deactivate();
            }
            //
            // Add the new blocks to the blockchain.  We will stop if we encounter
            // a missing block (this will happen if an invalid block is encountered
            // when downloading the blocks)
            //
            blockchain.writeLock();
            try {
                List<BlockImpl> forkBlocks = new ArrayList<>();
                for (int index = 1; index < chainBlockIds.size() && blockchain.getHeight() - startHeight < 720; index++) {
                    PeerBlock peerBlock = blockMap.get(chainBlockIds.get(index));
                    if (peerBlock == null) {
                        break;
                    }
                    BlockImpl block = peerBlock.getBlock();
                    if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
                        try {
                            pushBlock(block);
                        } catch (BlockNotAcceptedException e) {
                            peerBlock.getPeer().blacklist(e);
                        }
                    } else {
                        forkBlocks.add(block);
                    }
                }
                //
                // Process a fork
                //
                int myForkSize = blockchain.getHeight() - startHeight;
                if (!forkBlocks.isEmpty() && myForkSize < 720) {
                    Logger.logDebugMessage("Will process a fork of " + forkBlocks.size() + " blocks, mine is " + myForkSize);
                    processFork(feederPeer, forkBlocks, commonBlock);
                }
            } finally {
                blockchain.writeUnlock();
            }

        }

        private void processFork(final Peer peer, final List<BlockImpl> forkBlocks, final Block commonBlock) {

            BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();

            List<BlockImpl> myPoppedOffBlocks = popOffTo(commonBlock);

            int pushedForkBlocks = 0;
            if (blockchain.getLastBlock().getId() == commonBlock.getId()) {
                for (BlockImpl block : forkBlocks) {
                    if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
                        try {
                            pushBlock(block);
                            pushedForkBlocks += 1;
                        } catch (BlockNotAcceptedException e) {
                            peer.blacklist(e);
                            break;
                        }
                    }
                }
            }

            if (pushedForkBlocks > 0 && blockchain.getLastBlock().getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0) {
                Logger.logDebugMessage("Pop off caused by peer " + peer.getHost() + ", blacklisting");
                peer.blacklist("Pop off");
                List<BlockImpl> peerPoppedOffBlocks = popOffTo(commonBlock);
                pushedForkBlocks = 0;
                for (BlockImpl block : peerPoppedOffBlocks) {
                    TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                }
            }

            if (pushedForkBlocks == 0) {
                Logger.logDebugMessage("Didn't accept any blocks, pushing back my previous blocks");
                for (int i = myPoppedOffBlocks.size() - 1; i >= 0; i--) {
                    BlockImpl block = myPoppedOffBlocks.remove(i);
                    try {
                        pushBlock(block);
                    } catch (BlockNotAcceptedException e) {
                        Logger.logErrorMessage("Popped off block no longer acceptable: " + block.getJSONObject().toJSONString(), e);
                        break;
                    }
                }
            } else {
                Logger.logDebugMessage("Switched to peer's fork");
                for (BlockImpl block : myPoppedOffBlocks) {
                    TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                }
            }

        }

    };

    /**
     * Callable method to get the next block segment from the selected peer
     */
    private static class GetNextBlocks implements Callable<List<BlockImpl>> {

        /** Callable future */
        private Future<List<BlockImpl>> future;

        /** Peer */
        private Peer peer;

        /** Block identifier list */
        private final List<Long> blockIds;

        /** Start index */
        private int start;

        /** Stop index */
        private int stop;

        /** Request count */
        private int requestCount;

        /** Time it took to return getNextBlocks */
        private long responseTime;

        /**
         * Create the callable future
         *
         * @param   blockIds            Block identifier list
         * @param   start               Start index within the list
         * @param   stop                Stop index within the list
         */
        public GetNextBlocks(List<Long> blockIds, int start, int stop) {
            this.blockIds = blockIds;
            this.start = start;
            this.stop = stop;
            this.requestCount = 0;
        }

        /**
         * Return the result
         *
         * @return                      List of blocks or null if an error occurred
         */
        @Override
        public List<BlockImpl> call() {
            requestCount++;
            //
            // Build the block request list
            //
            JSONArray idList = new JSONArray();
            for (int i = start + 1; i <= stop; i++) {
                idList.add(Long.toUnsignedString(blockIds.get(i)));
            }
            JSONObject request = new JSONObject();
            request.put("requestType", "getNextBlocks");
            request.put("blockIds", idList);
            request.put("blockId", Long.toUnsignedString(blockIds.get(start)));
            long startTime = System.currentTimeMillis();
            JSONObject response = peer.send(JSON.prepareRequest(request), 10 * 1024 * 1024);
            responseTime = System.currentTimeMillis() - startTime;
            if (response == null) {
                return null;
            }
            //
            // Get the list of blocks.  We will stop parsing blocks if we encounter
            // an invalid block.  We will return the valid blocks and reset the stop
            // index so no more blocks will be processed.
            //
            List<JSONObject> nextBlocks = (List<JSONObject>)response.get("nextBlocks");
            if (nextBlocks == null)
                return null;
            if (nextBlocks.size() > 36) {
                Logger.logDebugMessage("Obsolete or rogue peer " + peer.getHost() + " sends too many nextBlocks, blacklisting");
                peer.blacklist("Too many nextBlocks");
                return null;
            }
            List<BlockImpl> blockList = new ArrayList<>(nextBlocks.size());
            try {
                int count = stop - start;
                for (JSONObject blockData : nextBlocks) {
                    blockList.add(BlockImpl.parseBlock(blockData));
                    if (--count <= 0)
                        break;
                }
            } catch (RuntimeException | WbxException.NotValidException e) {
                Logger.logDebugMessage("Failed to parse block: " + e.toString(), e);
                peer.blacklist(e);
                stop = start + blockList.size();
            }
            return blockList;
        }

        /**
         * Return the callable future
         *
         * @return                      Callable future
         */
        public Future<List<BlockImpl>> getFuture() {
            return future;
        }

        /**
         * Set the callable future
         *
         * @param   future              Callable future
         */
        public void setFuture(Future<List<BlockImpl>> future) {
            this.future = future;
        }

        /**
         * Return the peer
         *
         * @return                      Peer
         */
        public Peer getPeer() {
            return peer;
        }

        /**
         * Set the peer
         *
         * @param   peer                Peer
         */
        public void setPeer(Peer peer) {
            this.peer = peer;
        }

        /**
         * Return the start index
         *
         * @return                      Start index
         */
        public int getStart() {
            return start;
        }

        /**
         * Set the start index
         *
         * @param   start               Start index
         */
        public void setStart(int start) {
            this.start = start;
        }

        /**
         * Return the stop index
         *
         * @return                      Stop index
         */
        public int getStop() {
            return stop;
        }

        /**
         * Return the request count
         *
         * @return                      Request count
         */
        public int getRequestCount() {
            return requestCount;
        }

        /**
         * Return the response time
         *
         * @return                      Response time
         */
        public long getResponseTime() {
            return responseTime;
        }
    }

    /**
     * Block returned by a peer
     */
    private static class PeerBlock {

        /** Peer */
        private final Peer peer;

        /** Block */
        private final BlockImpl block;

        /**
         * Create the peer block
         *
         * @param   peer                Peer
         * @param   block               Block
         */
        public PeerBlock(Peer peer, BlockImpl block) {
            this.peer = peer;
            this.block = block;
        }

        /**
         * Return the peer
         *
         * @return                      Peer
         */
        public Peer getPeer() {
            return peer;
        }

        /**
         * Return the block
         *
         * @return                      Block
         */
        public BlockImpl getBlock() {
            return block;
        }
    }

    /**
     * Task to restore prunable data for downloaded blocks
     */
    private class RestorePrunableDataTask implements Runnable {

        @Override
        public void run() {
            Peer peer = null;
            try {
                //
                // Locate an archive peer
                //
                List<Peer> peers = Peers.getPeers(chkPeer -> chkPeer.providesService(Peer.Service.PRUNABLE) &&
                        !chkPeer.isBlacklisted() && chkPeer.getAnnouncedAddress() != null);
                while (!peers.isEmpty()) {
                    Peer chkPeer = peers.get(ThreadLocalRandom.current().nextInt(peers.size()));
                    if (chkPeer.getState() != Peer.State.CONNECTED) {
                        Peers.connectPeer(chkPeer);
                    }
                    if (chkPeer.getState() == Peer.State.CONNECTED) {
                        peer = chkPeer;
                        break;
                    }
                }
                if (peer == null) {
                    Logger.logDebugMessage("Cannot find any archive peers");
                    return;
                }
                Logger.logDebugMessage("Connected to archive peer " + peer.getHost());
                //
                // Make a copy of the prunable transaction list so we can remove entries
                // as we process them while still retaining the entry if we need to
                // retry later using a different archive peer
                //
                Set<Long> processing;
                synchronized (prunableTransactions) {
                    processing = new HashSet<>(prunableTransactions.size());
                    processing.addAll(prunableTransactions);
                }
                Logger.logDebugMessage("Need to restore " + processing.size() + " pruned data");
                //
                // Request transactions in batches of 100 until all transactions have been processed
                //
                while (!processing.isEmpty()) {
                    //
                    // Get the pruned transactions from the archive peer
                    //
                    JSONObject request = new JSONObject();
                    JSONArray requestList = new JSONArray();
                    synchronized (prunableTransactions) {
                        Iterator<Long> it = processing.iterator();
                        while (it.hasNext()) {
                            long id = it.next();
                            requestList.add(Long.toUnsignedString(id));
                            it.remove();
                            if (requestList.size() == 100)
                                break;
                        }
                    }
                    request.put("requestType", "getTransactions");
                    request.put("transactionIds", requestList);
                    JSONObject response = peer.send(JSON.prepareRequest(request), 10 * 1024 * 1024);
                    if (response == null) {
                        return;
                    }
                    //
                    // Restore the prunable data
                    //
                    JSONArray transactions = (JSONArray)response.get("transactions");
                    if (transactions == null || transactions.isEmpty()) {
                        return;
                    }
                    List<Transaction> processed = Wbx.getTransactionProcessor().restorePrunableData(transactions);
                    //
                    // Remove transactions that have been successfully processed
                    //
                    synchronized (prunableTransactions) {
                        processed.forEach(transaction -> prunableTransactions.remove(transaction.getId()));
                    }
                }
                Logger.logDebugMessage("Done retrieving prunable transactions from " + peer.getHost());
            } catch (WbxException.ValidationException e) {
                Logger.logErrorMessage("Peer " + peer.getHost() + " returned invalid prunable transaction", e);
                peer.blacklist(e);
            } catch (RuntimeException e) {
                Logger.logErrorMessage("Unable to restore prunable data", e);
            } finally {
                isRestoring = false;
                Logger.logDebugMessage("Remaining " + prunableTransactions.size() + " pruned transactions");
            }
        }
    }

    private final Listener<Block> checksumListener = block -> {
        if (block.getHeight() == Constants.TRANSPARENT_FORGING_BLOCK) {
            if (! verifyChecksum(CHECKSUM_TRANSPARENT_FORGING, 0, Constants.TRANSPARENT_FORGING_BLOCK)) {
                popOffTo(0);
            }
        } else if (block.getHeight() == Constants.NQT_BLOCK) {
            if (! verifyChecksum(CHECKSUM_NQT_BLOCK, Constants.TRANSPARENT_FORGING_BLOCK, Constants.NQT_BLOCK)) {
                popOffTo(Constants.TRANSPARENT_FORGING_BLOCK);
            }
        } else if (block.getHeight() == Constants.MONETARY_SYSTEM_BLOCK) {
            if (! verifyChecksum(CHECKSUM_MONETARY_SYSTEM_BLOCK, Constants.NQT_BLOCK, Constants.MONETARY_SYSTEM_BLOCK)) {
                popOffTo(Constants.NQT_BLOCK);
            }
        } else if (block.getHeight() == Constants.PHASING_BLOCK) {
            if (! verifyChecksum(CHECKSUM_PHASING_BLOCK, Constants.MONETARY_SYSTEM_BLOCK, Constants.PHASING_BLOCK)) {
                popOffTo(Constants.MONETARY_SYSTEM_BLOCK);
            }
        } else if (block.getHeight() == Constants.CHECKSUM_BLOCK_16) {
            if (! verifyChecksum(CHECKSUM_16, Constants.PHASING_BLOCK, Constants.CHECKSUM_BLOCK_16)) {
                popOffTo(Constants.PHASING_BLOCK);
            }
        } else if (block.getHeight() == Constants.CHECKSUM_BLOCK_17) {
            if (! verifyChecksum(CHECKSUM_17, Constants.CHECKSUM_BLOCK_16, Constants.CHECKSUM_BLOCK_17)) {
                popOffTo(Constants.CHECKSUM_BLOCK_16);
            }
        } else if (block.getHeight() == Constants.CHECKSUM_BLOCK_18) {
            if (! verifyChecksum(CHECKSUM_18, Constants.CHECKSUM_BLOCK_17, Constants.CHECKSUM_BLOCK_18)) {
                popOffTo(Constants.CHECKSUM_BLOCK_17);
            }
        } else if (block.getHeight() == Constants.CHECKSUM_BLOCK_19) {
            if (! verifyChecksum(CHECKSUM_19, Constants.CHECKSUM_BLOCK_18, Constants.CHECKSUM_BLOCK_19)) {
                popOffTo(Constants.CHECKSUM_BLOCK_18);
            }
        } else if (block.getHeight() == Constants.CHECKSUM_BLOCK_20) {
            if (! verifyChecksum(CHECKSUM_20, Constants.CHECKSUM_BLOCK_19, Constants.CHECKSUM_BLOCK_20)) {
                popOffTo(Constants.CHECKSUM_BLOCK_19);
            }
        } else if (block.getHeight() == Constants.CHECKSUM_BLOCK_21) {
            if (! verifyChecksum(CHECKSUM_21, Constants.CHECKSUM_BLOCK_20, Constants.CHECKSUM_BLOCK_21)) {
                popOffTo(Constants.CHECKSUM_BLOCK_20);
            }
        }
    };

    private BlockchainProcessorImpl() {
        final int trimFrequency = Wbx.getIntProperty("wbx.trimFrequency");
        blockListeners.addListener(block -> {
            if (block.getHeight() % 5000 == 0) {
                Logger.logMessage("processed block " + block.getHeight());
            }
            if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
                doTrimDerivedTables();
            }
        }, Event.BLOCK_SCANNED);

        blockListeners.addListener(block -> {
            if (trimDerivedTables && block.getHeight() % trimFrequency == 0 && !isTrimming) {
                isTrimming = true;
                networkService.submit(() -> {
                    trimDerivedTables();
                    isTrimming = false;
                });
            }
            if (block.getHeight() % 5000 == 0) {
                Logger.logMessage("received block " + block.getHeight());
                if (!isDownloading || block.getHeight() % 50000 == 0) {
                    networkService.submit(Db.db::analyzeTables);
                }
            }
        }, Event.BLOCK_PUSHED);

        blockListeners.addListener(checksumListener, Event.BLOCK_PUSHED);

        blockListeners.addListener(block -> Db.db.analyzeTables(), Event.RESCAN_END);

        ThreadPool.runBeforeStart(() -> {
            alreadyInitialized = true;
            if (addGenesisBlock()) {
                scan(0, false);
            } else if (Wbx.getBooleanProperty("wbx.forceScan")) {
                scan(0, Wbx.getBooleanProperty("wbx.forceValidate"));
            } else {
                boolean rescan;
                boolean validate;
                int height;
                try (Connection con = Db.db.getConnection();
                     Statement stmt = con.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM scan")) {
                    rs.next();
                    rescan = rs.getBoolean("rescan");
                    validate = rs.getBoolean("validate");
                    height = rs.getInt("height");
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                if (rescan) {
                    scan(height, validate);
                }
            }
        }, false);

        if (!Constants.isLightClient && !Constants.isOffline) {
            ThreadPool.scheduleThread("GetMoreBlocks", getMoreBlocksThread, 1);
        }

    }

    @Override
    public boolean addListener(Listener<Block> listener, BlockchainProcessor.Event eventType) {
        return blockListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<Block> listener, Event eventType) {
        return blockListeners.removeListener(listener, eventType);
    }

    @Override
    public void registerDerivedTable(DerivedDbTable table) {
        if (alreadyInitialized) {
            throw new IllegalStateException("Too late to register table " + table + ", must have done it in Wbx.Init");
        }
        derivedTables.add(table);
    }

    @Override
    public void trimDerivedTables() {
        try {
            Db.db.beginTransaction();
            doTrimDerivedTables();
            Db.db.commitTransaction();
        } catch (Exception e) {
            Logger.logMessage(e.toString(), e);
            Db.db.rollbackTransaction();
            throw e;
        } finally {
            Db.db.endTransaction();
        }
    }

    private void doTrimDerivedTables() {
        lastTrimHeight = Math.max(blockchain.getHeight() - Constants.MAX_ROLLBACK, 0);
        if (lastTrimHeight > 0) {
            for (DerivedDbTable table : derivedTables) {
                blockchain.readLock();
                try {
                    table.trim(lastTrimHeight);
                    Db.db.commitTransaction();
                } finally {
                    blockchain.readUnlock();
                }
            }
        }
    }

    List<DerivedDbTable> getDerivedTables() {
        return derivedTables;
    }

    @Override
    public Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    @Override
    public int getLastBlockchainFeederHeight() {
        return lastBlockchainFeederHeight;
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public int getInitialScanHeight() {
        return initialScanHeight;
    }

    @Override
    public boolean isDownloading() {
        return isDownloading;
    }

    @Override
    public boolean isProcessingBlock() {
        return isProcessingBlock;
    }

    @Override
    public int getMinRollbackHeight() {
        return trimDerivedTables ? (lastTrimHeight > 0 ? lastTrimHeight : Math.max(blockchain.getHeight() - Constants.MAX_ROLLBACK, 0)) : 0;
    }

    @Override
    public void processPeerBlock(JSONObject request) throws WbxException {
        BlockImpl block = BlockImpl.parseBlock(request);
        BlockImpl lastBlock = blockchain.getLastBlock();
        if (block.getPreviousBlockId() == lastBlock.getId()) {
            pushBlock(block);
        } else if (block.getPreviousBlockId() == lastBlock.getPreviousBlockId() && block.getTimestamp() < lastBlock.getTimestamp()) {
            blockchain.writeLock();
            try {
                if (lastBlock.getId() != blockchain.getLastBlock().getId()) {
                    return; // blockchain changed, ignore the block
                }
                BlockImpl previousBlock = blockchain.getBlock(lastBlock.getPreviousBlockId());
                lastBlock = popOffTo(previousBlock).get(0);
                try {
                    pushBlock(block);
                    TransactionProcessorImpl.getInstance().processLater(lastBlock.getTransactions());
                    Logger.logDebugMessage("Last block " + lastBlock.getStringId() + " was replaced by " + block.getStringId());
                } catch (BlockNotAcceptedException e) {
                    Logger.logDebugMessage("Replacement block failed to be accepted, pushing back our last block");
                    pushBlock(lastBlock);
                    TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                }
            } finally {
                blockchain.writeUnlock();
            }
        } // else ignore the block
    }

    @Override
    public List<BlockImpl> popOffTo(int height) {
        if (height <= 0) {
            fullReset();
        } else if (height < blockchain.getHeight()) {
            return popOffTo(blockchain.getBlockAtHeight(height));
        }
        return Collections.emptyList();
    }

    @Override
    public void fullReset() {
        blockchain.writeLock();
        try {
            try {
                setGetMoreBlocks(false);
                scheduleScan(0, false);
                //BlockDb.deleteBlock(Genesis.GENESIS_BLOCK_ID); // fails with stack overflow in H2
                BlockDb.deleteAll();
                if (addGenesisBlock()) {
                    scan(0, false);
                }
            } finally {
                setGetMoreBlocks(true);
            }
        } finally {
            blockchain.writeUnlock();
        }
    }

    @Override
    public void setGetMoreBlocks(boolean getMoreBlocks) {
        this.getMoreBlocks = getMoreBlocks;
    }

    @Override
    public int restorePrunedData() {
        Db.db.beginTransaction();
        try (Connection con = Db.db.getConnection()) {
            int now = Wbx.getEpochTime();
            int minTimestamp = Math.max(1, now - Constants.MAX_PRUNABLE_LIFETIME);
            int maxTimestamp = Math.max(minTimestamp, now - Constants.MIN_PRUNABLE_LIFETIME) - 1;
            List<TransactionDb.PrunableTransaction> transactionList =
                    TransactionDb.findPrunableTransactions(con, minTimestamp, maxTimestamp);
            transactionList.forEach(prunableTransaction -> {
                long id = prunableTransaction.getId();
                if ((prunableTransaction.hasPrunableAttachment() && prunableTransaction.getTransactionType().isPruned(id)) ||
                        PrunableMessage.isPruned(id, prunableTransaction.hasPrunablePlainMessage(), prunableTransaction.hasPrunableEncryptedMessage())) {
                    synchronized (prunableTransactions) {
                        prunableTransactions.add(id);
                    }
                }
            });
            if (!prunableTransactions.isEmpty()) {
                lastRestoreTime = 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            Db.db.endTransaction();
        }
        synchronized (prunableTransactions) {
            return prunableTransactions.size();
        }
    }

    @Override
    public Transaction restorePrunedTransaction(long transactionId) {
        TransactionImpl transaction = TransactionDb.findTransaction(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        boolean isPruned = false;
        for (Appendix.AbstractAppendix appendage : transaction.getAppendages(true)) {
            if ((appendage instanceof Appendix.Prunable) &&
                    !((Appendix.Prunable)appendage).hasPrunableData()) {
                isPruned = true;
                break;
            }
        }
        if (!isPruned) {
            return transaction;
        }
        List<Peer> peers = Peers.getPeers(chkPeer -> chkPeer.providesService(Peer.Service.PRUNABLE) &&
                !chkPeer.isBlacklisted() && chkPeer.getAnnouncedAddress() != null);
        if (peers.isEmpty()) {
            Logger.logDebugMessage("Cannot find any archive peers");
            return null;
        }
        JSONObject json = new JSONObject();
        JSONArray requestList = new JSONArray();
        requestList.add(Long.toUnsignedString(transactionId));
        json.put("requestType", "getTransactions");
        json.put("transactionIds", requestList);
        JSONStreamAware request = JSON.prepareRequest(json);
        for (Peer peer : peers) {
            if (peer.getState() != Peer.State.CONNECTED) {
                Peers.connectPeer(peer);
            }
            if (peer.getState() != Peer.State.CONNECTED) {
                continue;
            }
            Logger.logDebugMessage("Connected to archive peer " + peer.getHost());
            JSONObject response = peer.send(request);
            if (response == null) {
                continue;
            }
            JSONArray transactions = (JSONArray)response.get("transactions");
            if (transactions == null || transactions.isEmpty()) {
                continue;
            }
            try {
                List<Transaction> processed = Wbx.getTransactionProcessor().restorePrunableData(transactions);
                if (processed.isEmpty()) {
                    continue;
                }
                synchronized (prunableTransactions) {
                    prunableTransactions.remove(transactionId);
                }
                return processed.get(0);
            } catch (WbxException.NotValidException e) {
                Logger.logErrorMessage("Peer " + peer.getHost() + " returned invalid prunable transaction", e);
                peer.blacklist(e);
            }
        }
        return null;
    }

    void shutdown() {
        ThreadPool.shutdownExecutor("networkService", networkService, 5);
    }

    private void addBlock(BlockImpl block) {
        try (Connection con = Db.db.getConnection()) {
            BlockDb.saveBlock(con, block);
            blockchain.setLastBlock(block);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private boolean addGenesisBlock() {
        if (BlockDb.hasBlock(Genesis.GENESIS_BLOCK_ID, 0)) {
            Logger.logMessage("Genesis block already in database");
            BlockImpl lastBlock = BlockDb.findLastBlock();
            blockchain.setLastBlock(lastBlock);
            popOffTo(lastBlock);
            Logger.logMessage("Last block height: " + lastBlock.getHeight());
            return false;
        }
        Logger.logMessage("Genesis block not in database, starting from scratch");
        try {
            List<TransactionImpl> transactions = new ArrayList<>();
            for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++) {
                TransactionImpl transaction = new TransactionImpl.BuilderImpl((byte) 0, Genesis.CREATOR_PUBLIC_KEY,
                        Genesis.GENESIS_AMOUNTS[i] * Constants.ONE_WIN, 0, (short) 0,
                        Attachment.ORDINARY_PAYMENT)
                        .timestamp(0)
                        .recipientId(Genesis.GENESIS_RECIPIENTS[i])
                        .signature(Genesis.GENESIS_SIGNATURES[i])
                        .height(0)
                        .ecBlockHeight(0)
                        .ecBlockId(0)
                        .build();
                transactions.add(transaction);
            }
            Collections.sort(transactions, Comparator.comparingLong(Transaction::getId));
            MessageDigest digest = Crypto.sha256();
            for (TransactionImpl transaction : transactions) {
                digest.update(transaction.bytes());
            }
            BlockImpl genesisBlock = new BlockImpl(-1, 0, 0, Constants.MAX_BALANCE_NQT, 0, transactions.size() * 128, digest.digest(),
                    Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE, null, transactions);
            genesisBlock.setPrevious(null);
            //suse
            Logger.logInfoMessage("######################################################################");
            Logger.logInfoMessage(Constants.COIN_NAME + ": Genesis block id=" + genesisBlock.getId());
            Logger.logInfoMessage("######################################################################");
            addBlock(genesisBlock);
            return true;
        } catch (WbxException.ValidationException e) {
            Logger.logMessage(e.getMessage());
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void pushBlock(final BlockImpl block) throws BlockNotAcceptedException {

        int curTime = Wbx.getEpochTime();

        blockchain.writeLock();
        try {
            BlockImpl previousLastBlock = null;
            try {
                Db.db.beginTransaction();
                previousLastBlock = blockchain.getLastBlock();

                validate(block, previousLastBlock, curTime);

                long nextHitTime = Generator.getNextHitTime(previousLastBlock.getId(), curTime);
                if (nextHitTime > 0 && block.getTimestamp() > nextHitTime + 1) {
                    String msg = "Rejecting block " + block.getStringId() + " at height " + previousLastBlock.getHeight()
                            + " block timestamp " + block.getTimestamp() + " next hit time " + nextHitTime
                            + " current time " + curTime;
                    Logger.logDebugMessage(msg);
                    Generator.setDelay(-Constants.FORGING_SPEEDUP);
                    throw new BlockOutOfOrderException(msg, block);
                }

                Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
                List<TransactionImpl> validPhasedTransactions = new ArrayList<>();
                List<TransactionImpl> invalidPhasedTransactions = new ArrayList<>();
                validatePhasedTransactions(previousLastBlock.getHeight(), validPhasedTransactions, invalidPhasedTransactions, duplicates);
                validateTransactions(block, previousLastBlock, curTime, duplicates, previousLastBlock.getHeight() >= Constants.LAST_CHECKSUM_BLOCK);

                block.setPrevious(previousLastBlock);
                blockListeners.notify(block, Event.BEFORE_BLOCK_ACCEPT);
                TransactionProcessorImpl.getInstance().requeueAllUnconfirmedTransactions();
                addBlock(block);
                accept(block, validPhasedTransactions, invalidPhasedTransactions, duplicates);

                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                blockchain.setLastBlock(previousLastBlock);
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            blockListeners.notify(block, Event.AFTER_BLOCK_ACCEPT);
        } finally {
            blockchain.writeUnlock();
        }

        if (block.getTimestamp() >= curTime - 600) {
            Peers.sendToSomePeers(block);
        }

        blockListeners.notify(block, Event.BLOCK_PUSHED);

    }

    private void validatePhasedTransactions(int height, List<TransactionImpl> validPhasedTransactions, List<TransactionImpl> invalidPhasedTransactions,
                                            Map<TransactionType, Map<String, Integer>> duplicates) {
        if (height >= Constants.PHASING_BLOCK) {
            try (DbIterator<TransactionImpl> phasedTransactions = PhasingPoll.getFinishingTransactions(height + 1)) {
                for (TransactionImpl phasedTransaction : phasedTransactions) {
                    if (height > Constants.SHUFFLING_BLOCK && PhasingPoll.getResult(phasedTransaction.getId()) != null) {
                        continue;
                    }
                    try {
                        phasedTransaction.validate();
                        if (!phasedTransaction.attachmentIsDuplicate(duplicates, false)) {
                            validPhasedTransactions.add(phasedTransaction);
                        } else {
                            Logger.logDebugMessage("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " is duplicate, will not apply");
                            invalidPhasedTransactions.add(phasedTransaction);
                        }
                    } catch (WbxException.ValidationException e) {
                        Logger.logDebugMessage("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " no longer passes validation: "
                                + e.getMessage() + ", will not apply");
                        invalidPhasedTransactions.add(phasedTransaction);
                    }
                }
            }
        }
    }

    private void validate(BlockImpl block, BlockImpl previousLastBlock, int curTime) throws BlockNotAcceptedException {
        if (previousLastBlock.getId() != block.getPreviousBlockId()) {
            throw new BlockOutOfOrderException("Previous block id doesn't match", block);
        }
        if (block.getVersion() != getBlockVersion(previousLastBlock.getHeight())) {
            throw new BlockNotAcceptedException("Invalid version " + block.getVersion(), block);
        }
        if (block.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
            Logger.logWarningMessage("Received block " + block.getStringId() + " from the future, timestamp " + block.getTimestamp()
                    + " generator " + Long.toUnsignedString(block.getGeneratorId()) + " current time " + curTime + ", system clock may be off");
            throw new BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
                    + " current time is " + curTime, block);
        }
        if (block.getTimestamp() <= previousLastBlock.getTimestamp()) {
            throw new BlockNotAcceptedException("Block timestamp " + block.getTimestamp() + " is before previous block timestamp "
                    + previousLastBlock.getTimestamp(), block);
        }
        if (block.getVersion() != 1 && !Arrays.equals(Crypto.sha256().digest(previousLastBlock.bytes()), block.getPreviousBlockHash())) {
            throw new BlockNotAcceptedException("Previous block hash doesn't match", block);
        }
        if (block.getId() == 0L || BlockDb.hasBlock(block.getId(), previousLastBlock.getHeight())) {
            throw new BlockNotAcceptedException("Duplicate block or invalid id", block);
        }
        if (!block.verifyGenerationSignature() && !Generator.allowsFakeForging(block.getGeneratorPublicKey())) {
            Account generatorAccount = Account.getAccount(block.getGeneratorId());
            long generatorBalance = generatorAccount == null ? 0 : generatorAccount.getEffectiveBalanceWIN();
            throw new BlockNotAcceptedException("Generation signature verification failed, effective balance " + generatorBalance, block);
        }
        if (!block.verifyBlockSignature()) {
            throw new BlockNotAcceptedException("Block signature verification failed", block);
        }
        if (block.getTransactions().size() > Constants.MAX_NUMBER_OF_TRANSACTIONS) {
            throw new BlockNotAcceptedException("Invalid block transaction count " + block.getTransactions().size(), block);
        }
        if (block.getPayloadLength() > Constants.MAX_PAYLOAD_LENGTH || block.getPayloadLength() < 0) {
            throw new BlockNotAcceptedException("Invalid block payload length " + block.getPayloadLength(), block);
        }
    }

    private void validateTransactions(BlockImpl block, BlockImpl previousLastBlock, int curTime, Map<TransactionType, Map<String, Integer>> duplicates,
                                      boolean fullValidation) throws BlockNotAcceptedException {
        long payloadLength = 0;
        long calculatedTotalAmount = 0;
        long calculatedTotalFee = 0;
        MessageDigest digest = Crypto.sha256();
        boolean hasPrunedTransactions = false;
        for (TransactionImpl transaction : block.getTransactions()) {
            if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
                throw new BlockOutOfOrderException("Invalid transaction timestamp: " + transaction.getTimestamp()
                        + ", current time is " + curTime, block);
            }
            if (!transaction.verifySignature()) {
                throw new TransactionNotAcceptedException("Transaction signature verification failed at height " + previousLastBlock.getHeight(), transaction);
            }
            if (fullValidation) {
                // cfb: Block 303 contains a transaction which expired before the block timestamp
                if (transaction.getTimestamp() > block.getTimestamp() + Constants.MAX_TIMEDRIFT
                        || (transaction.getExpiration() < block.getTimestamp() && previousLastBlock.getHeight() != 303)) {
                    throw new TransactionNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
                            + ", current time is " + curTime + ", block timestamp is " + block.getTimestamp(), transaction);
                }
                if (TransactionDb.hasTransaction(transaction.getId(), previousLastBlock.getHeight())) {
                    throw new TransactionNotAcceptedException("Transaction is already in the blockchain", transaction);
                }
                if (transaction.referencedTransactionFullHash() != null) {
                    if ((previousLastBlock.getHeight() < Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                            && !TransactionDb.hasTransaction(Convert.fullHashToId(transaction.referencedTransactionFullHash()), previousLastBlock.getHeight()))
                            || (previousLastBlock.getHeight() >= Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                            && !hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0))) {
                        throw new TransactionNotAcceptedException("Missing or invalid referenced transaction "
                                + transaction.getReferencedTransactionFullHash(), transaction);
                    }
                }
                if (transaction.getVersion() != getTransactionVersion(previousLastBlock.getHeight())) {
                    throw new TransactionNotAcceptedException("Invalid transaction version " + transaction.getVersion()
                            + " at height " + previousLastBlock.getHeight(), transaction);
                }
                if (transaction.getId() == 0L) {
                    throw new TransactionNotAcceptedException("Invalid transaction id 0", transaction);
                }
                try {
                    transaction.validate();
                } catch (WbxException.ValidationException e) {
                    throw new TransactionNotAcceptedException(e.getMessage(), transaction);
                }
            }
            if (transaction.attachmentIsDuplicate(duplicates, true)) {
                throw new TransactionNotAcceptedException("Transaction is a duplicate", transaction);
            }
            if (!hasPrunedTransactions) {
                for (Appendix.AbstractAppendix appendage : transaction.getAppendages()) {
                    if ((appendage instanceof Appendix.Prunable) && !((Appendix.Prunable)appendage).hasPrunableData()) {
                        hasPrunedTransactions = true;
                        break;
                    }
                }
            }
            calculatedTotalAmount += transaction.getAmountNQT();
            calculatedTotalFee += transaction.getFeeNQT();
            payloadLength += transaction.getFullSize();
            digest.update(transaction.bytes());
        }
        if (calculatedTotalAmount != block.getTotalAmountNQT() || calculatedTotalFee != block.getTotalFeeNQT()) {
            throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals", block);
        }
        if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
            throw new BlockNotAcceptedException("Payload hash doesn't match", block);
        }
        if (hasPrunedTransactions ? payloadLength > block.getPayloadLength() : payloadLength != block.getPayloadLength()) {
            throw new BlockNotAcceptedException("Transaction payload length " + payloadLength + " does not match block payload length "
                    + block.getPayloadLength(), block);
        }
    }

    private void accept(BlockImpl block, List<TransactionImpl> validPhasedTransactions, List<TransactionImpl> invalidPhasedTransactions,
                        Map<TransactionType, Map<String, Integer>> duplicates) throws TransactionNotAcceptedException {
        try {
            isProcessingBlock = true;
            for (TransactionImpl transaction : block.getTransactions()) {
                if (! transaction.applyUnconfirmed()) {
                    throw new TransactionNotAcceptedException("Double spending", transaction);
                }
            }
            blockListeners.notify(block, Event.BEFORE_BLOCK_APPLY);
            block.apply();
            validPhasedTransactions.forEach(transaction -> transaction.getPhasing().countVotes(transaction));
            invalidPhasedTransactions.forEach(transaction -> transaction.getPhasing().reject(transaction));
            int fromTimestamp = Wbx.getEpochTime() - Constants.MAX_PRUNABLE_LIFETIME;
            for (TransactionImpl transaction : block.getTransactions()) {
                try {
                    transaction.apply();
                    if (transaction.getTimestamp() > fromTimestamp) {
                        for (Appendix.AbstractAppendix appendage : transaction.getAppendages(true)) {
                            if ((appendage instanceof Appendix.Prunable) &&
                                        !((Appendix.Prunable)appendage).hasPrunableData()) {
                                synchronized (prunableTransactions) {
                                    prunableTransactions.add(transaction.getId());
                                }
                                lastRestoreTime = 0;
                                break;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    Logger.logErrorMessage(e.toString(), e);
                    throw new BlockchainProcessor.TransactionNotAcceptedException(e, transaction);
                }
            }
            if (block.getHeight() > Constants.SHUFFLING_BLOCK) {
                SortedSet<TransactionImpl> possiblyApprovedTransactions = new TreeSet<>(finishingTransactionsComparator);
                block.getTransactions().forEach(transaction -> {
                    PhasingPoll.getLinkedPhasedTransactions(transaction.fullHash()).forEach(phasedTransaction -> {
                        if (phasedTransaction.getPhasing().getFinishHeight() > block.getHeight()) {
                            possiblyApprovedTransactions.add((TransactionImpl)phasedTransaction);
                        }
                    });
                    if (transaction.getType() == TransactionType.Messaging.PHASING_VOTE_CASTING && !transaction.attachmentIsPhased()) {
                        Attachment.MessagingPhasingVoteCasting voteCasting = (Attachment.MessagingPhasingVoteCasting)transaction.getAttachment();
                        voteCasting.getTransactionFullHashes().forEach(hash -> {
                            PhasingPoll phasingPoll = PhasingPoll.getPoll(Convert.fullHashToId(hash));
                            if (phasingPoll.allowEarlyFinish() && phasingPoll.getFinishHeight() > block.getHeight()) {
                                possiblyApprovedTransactions.add(TransactionDb.findTransaction(phasingPoll.getId()));
                            }
                        });
                    }
                });
                validPhasedTransactions.forEach(phasedTransaction -> {
                    if (phasedTransaction.getType() == TransactionType.Messaging.PHASING_VOTE_CASTING) {
                        PhasingPoll.PhasingPollResult result = PhasingPoll.getResult(phasedTransaction.getId());
                        if (result != null && result.isApproved()) {
                            Attachment.MessagingPhasingVoteCasting phasingVoteCasting = (Attachment.MessagingPhasingVoteCasting) phasedTransaction.getAttachment();
                            phasingVoteCasting.getTransactionFullHashes().forEach(hash -> {
                                PhasingPoll phasingPoll = PhasingPoll.getPoll(Convert.fullHashToId(hash));
                                if (phasingPoll.allowEarlyFinish() && phasingPoll.getFinishHeight() > block.getHeight()) {
                                    possiblyApprovedTransactions.add(TransactionDb.findTransaction(phasingPoll.getId()));
                                }
                            });
                        }
                    }
                });
                possiblyApprovedTransactions.forEach(transaction -> {
                    if (PhasingPoll.getResult(transaction.getId()) == null) {
                        try {
                            transaction.validate();
                            transaction.getPhasing().tryCountVotes(transaction, duplicates);
                        } catch (WbxException.ValidationException e) {
                            Logger.logDebugMessage("At height " + block.getHeight() + " phased transaction " + transaction.getStringId()
                                    + " no longer passes validation: " + e.getMessage() + ", cannot finish early");
                        }
                    }
                });
            }
            blockListeners.notify(block, Event.AFTER_BLOCK_APPLY);
            if (block.getTransactions().size() > 0) {
                TransactionProcessorImpl.getInstance().notifyListeners(block.getTransactions(), TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
            }
            AccountLedger.commitEntries();
        } finally {
            isProcessingBlock = false;
            AccountLedger.clearEntries();
        }
    }

    private static final Comparator<Transaction> finishingTransactionsComparator = Comparator
            .comparingInt(Transaction::getHeight)
            .thenComparingInt(Transaction::getIndex)
            .thenComparingLong(Transaction::getId);

    List<BlockImpl> popOffTo(Block commonBlock) {
        blockchain.writeLock();
        try {
            if (!Db.db.isInTransaction()) {
                try {
                    Db.db.beginTransaction();
                    return popOffTo(commonBlock);
                } finally {
                    Db.db.endTransaction();
                }
            }
            if (commonBlock.getHeight() < getMinRollbackHeight()) {
                Logger.logMessage("Rollback to height " + commonBlock.getHeight() + " not supported, will do a full rescan");
                popOffWithRescan(commonBlock.getHeight() + 1);
                return Collections.emptyList();
            }
            if (! blockchain.hasBlock(commonBlock.getId())) {
                Logger.logDebugMessage("Block " + commonBlock.getStringId() + " not found in blockchain, nothing to pop off");
                return Collections.emptyList();
            }
            List<BlockImpl> poppedOffBlocks = new ArrayList<>();
            try {
                BlockImpl block = blockchain.getLastBlock();
                block.loadTransactions();
                Logger.logDebugMessage("Rollback from block " + block.getStringId() + " at height " + block.getHeight()
                        + " to " + commonBlock.getStringId() + " at " + commonBlock.getHeight());
                while (block.getId() != commonBlock.getId() && block.getId() != Genesis.GENESIS_BLOCK_ID) {
                    poppedOffBlocks.add(block);
                    block = popLastBlock();
                }
                for (DerivedDbTable table : derivedTables) {
                    table.rollback(commonBlock.getHeight());
                }
                Db.db.clearCache();
                Db.db.commitTransaction();
            } catch (RuntimeException e) {
                Logger.logErrorMessage("Error popping off to " + commonBlock.getHeight() + ", " + e.toString());
                Db.db.rollbackTransaction();
                BlockImpl lastBlock = BlockDb.findLastBlock();
                blockchain.setLastBlock(lastBlock);
                popOffTo(lastBlock);
                throw e;
            }
            return poppedOffBlocks;
        } finally {
            blockchain.writeUnlock();
        }
    }

    private BlockImpl popLastBlock() {
        BlockImpl block = blockchain.getLastBlock();
        if (block.getId() == Genesis.GENESIS_BLOCK_ID) {
            throw new RuntimeException("Cannot pop off genesis block");
        }
        BlockImpl previousBlock = BlockDb.deleteBlocksFrom(block.getId());
        previousBlock.loadTransactions();
        blockchain.setLastBlock(previousBlock);
        blockListeners.notify(block, Event.BLOCK_POPPED);
        return previousBlock;
    }

    private void popOffWithRescan(int height) {
        blockchain.writeLock();
        try {
            try {
                scheduleScan(0, false);
                BlockImpl lastBLock = BlockDb.deleteBlocksFrom(BlockDb.findBlockIdAtHeight(height));
                blockchain.setLastBlock(lastBLock);
                Logger.logDebugMessage("Deleted blocks starting from height %s", height);
            } finally {
                scan(0, false);
            }
        } finally {
            blockchain.writeUnlock();
        }
    }

    private int getBlockVersion(int previousBlockHeight) {
        return previousBlockHeight < Constants.TRANSPARENT_FORGING_BLOCK ? 1
                : previousBlockHeight < Constants.NQT_BLOCK ? 2
                : 3;
    }

    private int getTransactionVersion(int previousBlockHeight) {
        return previousBlockHeight < Constants.DIGITAL_GOODS_STORE_BLOCK ? 0 : 1;
    }

    private boolean verifyChecksum(byte[] validChecksum, int fromHeight, int toHeight) {
        MessageDigest digest = Crypto.sha256();
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "SELECT * FROM transaction WHERE height > ? AND height <= ? ORDER BY id ASC, timestamp ASC")) {
            pstmt.setInt(1, fromHeight);
            pstmt.setInt(2, toHeight);
            try (DbIterator<TransactionImpl> iterator = blockchain.getTransactions(con, pstmt)) {
                while (iterator.hasNext()) {
                    digest.update(iterator.next().bytes());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        byte[] checksum = digest.digest();
        if (validChecksum == null) {
            Logger.logMessage("Checksum calculated:\n" + Arrays.toString(checksum));
            return true;
        } else if (!Arrays.equals(checksum, validChecksum)) {
            Logger.logErrorMessage("Checksum failed at block " + blockchain.getHeight() + ": " + Arrays.toString(checksum));
            return false;
        } else {
            Logger.logMessage("Checksum passed at block " + blockchain.getHeight());
            return true;
        }
    }

    SortedSet<UnconfirmedTransaction> selectUnconfirmedTransactions(Map<TransactionType, Map<String, Integer>> duplicates, Block previousBlock, int blockTimestamp) {
        List<UnconfirmedTransaction> orderedUnconfirmedTransactions = new ArrayList<>();
        try (FilteringIterator<UnconfirmedTransaction> unconfirmedTransactions = new FilteringIterator<>(
                TransactionProcessorImpl.getInstance().getAllUnconfirmedTransactions(),
                transaction -> hasAllReferencedTransactions(transaction.getTransaction(), transaction.getTimestamp(), 0))) {
            for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                orderedUnconfirmedTransactions.add(unconfirmedTransaction);
            }
        }
        SortedSet<UnconfirmedTransaction> sortedTransactions = new TreeSet<>(transactionArrivalComparator);
        int payloadLength = 0;
        while (payloadLength <= Constants.MAX_PAYLOAD_LENGTH && sortedTransactions.size() <= Constants.MAX_NUMBER_OF_TRANSACTIONS) {
            int prevNumberOfNewTransactions = sortedTransactions.size();
            for (UnconfirmedTransaction unconfirmedTransaction : orderedUnconfirmedTransactions) {
                int transactionLength = unconfirmedTransaction.getTransaction().getFullSize();
                if (sortedTransactions.contains(unconfirmedTransaction) || payloadLength + transactionLength > Constants.MAX_PAYLOAD_LENGTH) {
                    continue;
                }
                if (unconfirmedTransaction.getVersion() != getTransactionVersion(previousBlock.getHeight())) {
                    continue;
                }
                if (blockTimestamp > 0 && (unconfirmedTransaction.getTimestamp() > blockTimestamp + Constants.MAX_TIMEDRIFT
                        || unconfirmedTransaction.getExpiration() < blockTimestamp)) {
                    continue;
                }
                try {
                    unconfirmedTransaction.getTransaction().validate();
                } catch (WbxException.ValidationException e) {
                    continue;
                }
                if (unconfirmedTransaction.getTransaction().attachmentIsDuplicate(duplicates, true)) {
                    continue;
                }
                sortedTransactions.add(unconfirmedTransaction);
                payloadLength += transactionLength;
            }
            if (sortedTransactions.size() == prevNumberOfNewTransactions) {
                break;
            }
        }
        return sortedTransactions;
    }


    private static final Comparator<UnconfirmedTransaction> transactionArrivalComparator = Comparator
            .comparingLong(UnconfirmedTransaction::getArrivalTimestamp)
            .thenComparingInt(UnconfirmedTransaction::getHeight)
            .thenComparingLong(UnconfirmedTransaction::getId);

    void generateBlock(String secretPhrase, int blockTimestamp) throws BlockNotAcceptedException {

        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        if (blockchain.getHeight() >= Constants.PHASING_BLOCK) {
            try (DbIterator<TransactionImpl> phasedTransactions = PhasingPoll.getFinishingTransactions(blockchain.getHeight() + 1)) {
                for (TransactionImpl phasedTransaction : phasedTransactions) {
                    try {
                        phasedTransaction.validate();
                        phasedTransaction.attachmentIsDuplicate(duplicates, false); // pre-populate duplicates map
                    } catch (WbxException.ValidationException ignore) {
                    }
                }
            }
        }

        BlockImpl previousBlock = blockchain.getLastBlock();
        TransactionProcessorImpl.getInstance().processWaitingTransactions();
        SortedSet<UnconfirmedTransaction> sortedTransactions = selectUnconfirmedTransactions(duplicates, previousBlock, blockTimestamp);
        List<TransactionImpl> blockTransactions = new ArrayList<>();
        MessageDigest digest = Crypto.sha256();
        long totalAmountNQT = 0;
        long totalFeeNQT = 0;
        int payloadLength = 0;
        for (UnconfirmedTransaction unconfirmedTransaction : sortedTransactions) {
            TransactionImpl transaction = unconfirmedTransaction.getTransaction();
            blockTransactions.add(transaction);
            digest.update(transaction.bytes());
            totalAmountNQT += transaction.getAmountNQT();
            totalFeeNQT += transaction.getFeeNQT();
            payloadLength += transaction.getFullSize();
        }
        byte[] payloadHash = digest.digest();
        digest.update(previousBlock.getGenerationSignature());
        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        byte[] generationSignature = digest.digest(publicKey);
        byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.bytes());

        BlockImpl block = new BlockImpl(getBlockVersion(previousBlock.getHeight()), blockTimestamp, previousBlock.getId(), totalAmountNQT, totalFeeNQT, payloadLength,
                payloadHash, publicKey, generationSignature, previousBlockHash, blockTransactions, secretPhrase);

        try {
            pushBlock(block);
            blockListeners.notify(block, Event.BLOCK_GENERATED);
            Logger.logDebugMessage("Account " + Long.toUnsignedString(block.getGeneratorId()) + " generated block " + block.getStringId()
                    + " at height " + block.getHeight() + " timestamp " + block.getTimestamp() + " fee " + ((float)block.getTotalFeeNQT())/Constants.ONE_WIN);
        } catch (TransactionNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
            TransactionProcessorImpl.getInstance().processWaitingTransactions();
            TransactionImpl transaction = e.getTransaction();
            Logger.logDebugMessage("Removing invalid transaction: " + transaction.getStringId());
            blockchain.writeLock();
            try {
                TransactionProcessorImpl.getInstance().removeUnconfirmedTransaction(transaction);
            } finally {
                blockchain.writeUnlock();
            }
            throw e;
        } catch (BlockNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
            throw e;
        }
    }

    boolean hasAllReferencedTransactions(TransactionImpl transaction, int timestamp, int count) {
        if (transaction.referencedTransactionFullHash() == null) {
            return timestamp - transaction.getTimestamp() < Constants.MAX_REFERENCED_TRANSACTION_TIMESPAN && count < 10;
        }
        TransactionImpl referencedTransaction = TransactionDb.findTransactionByFullHash(transaction.referencedTransactionFullHash());
        return referencedTransaction != null
                && referencedTransaction.getHeight() < transaction.getHeight()
                && hasAllReferencedTransactions(referencedTransaction, timestamp, count + 1);
    }

    void scheduleScan(int height, boolean validate) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE scan SET rescan = TRUE, height = ?, validate = ?")) {
            pstmt.setInt(1, height);
            pstmt.setBoolean(2, validate);
            pstmt.executeUpdate();
            Logger.logDebugMessage("Scheduled scan starting from height " + height + (validate ? ", with validation" : ""));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void scan(int height, boolean validate) {
        scan(height, validate, false);
    }

    @Override
    public void fullScanWithShutdown() {
        scan(0, true, true);
    }

    private void scan(int height, boolean validate, boolean shutdown) {
        blockchain.writeLock();
        try {
            if (!Db.db.isInTransaction()) {
                try {
                    Db.db.beginTransaction();
                    if (validate) {
                        blockListeners.addListener(checksumListener, Event.BLOCK_SCANNED);
                    }
                    scan(height, validate, shutdown);
                    Db.db.commitTransaction();
                } catch (Exception e) {
                    Db.db.rollbackTransaction();
                    throw e;
                } finally {
                    Db.db.endTransaction();
                    blockListeners.removeListener(checksumListener, Event.BLOCK_SCANNED);
                }
                return;
            }
            scheduleScan(height, validate);
            if (height > 0 && height < getMinRollbackHeight()) {
                Logger.logMessage("Rollback to height less than " + getMinRollbackHeight() + " not supported, will do a full scan");
                height = 0;
            }
            if (height < 0) {
                height = 0;
            }
            Logger.logMessage("Scanning blockchain starting from height " + height + "...");
            if (validate) {
                Logger.logDebugMessage("Also verifying signatures and validating transactions...");
            }
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmtSelect = con.prepareStatement("SELECT * FROM block WHERE " + (height > 0 ? "height >= ? AND " : "")
                         + " db_id >= ? ORDER BY db_id ASC LIMIT 50000");
                 PreparedStatement pstmtDone = con.prepareStatement("UPDATE scan SET rescan = FALSE, height = 0, validate = FALSE")) {
                isScanning = true;
                initialScanHeight = blockchain.getHeight();
                if (height > blockchain.getHeight() + 1) {
                    Logger.logMessage("Rollback height " + (height - 1) + " exceeds current blockchain height of " + blockchain.getHeight() + ", no scan needed");
                    pstmtDone.executeUpdate();
                    Db.db.commitTransaction();
                    return;
                }
                if (height == 0) {
                    Logger.logDebugMessage("Dropping all full text search indexes");
                    FullTextTrigger.dropAll(con);
                }
                for (DerivedDbTable table : derivedTables) {
                    if (height == 0) {
                        table.truncate();
                    } else {
                        table.rollback(height - 1);
                    }
                }
                Db.db.clearCache();
                Db.db.commitTransaction();
                Logger.logDebugMessage("Rolled back derived tables");
                BlockImpl currentBlock = BlockDb.findBlockAtHeight(height);
                blockListeners.notify(currentBlock, Event.RESCAN_BEGIN);
                long currentBlockId = currentBlock.getId();
                if (height == 0) {
                    blockchain.setLastBlock(currentBlock); // special case to avoid no last block
                    Account.addOrGetAccount(Genesis.CREATOR_ID).apply(Genesis.CREATOR_PUBLIC_KEY);
                } else {
                    blockchain.setLastBlock(BlockDb.findBlockAtHeight(height - 1));
                }
                if (shutdown) {
                    Logger.logMessage("Scan will be performed at next start");
                    new Thread(() -> System.exit(0)).start();
                    return;
                }
                int pstmtSelectIndex = 1;
                if (height > 0) {
                    pstmtSelect.setInt(pstmtSelectIndex++, height);
                }
                long dbId = Long.MIN_VALUE;
                boolean hasMore = true;
                outer:
                while (hasMore) {
                    hasMore = false;
                    pstmtSelect.setLong(pstmtSelectIndex, dbId);
                    try (ResultSet rs = pstmtSelect.executeQuery()) {
                        while (rs.next()) {
                            try {
                                dbId = rs.getLong("db_id");
                                currentBlock = BlockDb.loadBlock(con, rs, true);
                                currentBlock.loadTransactions();
                                if (currentBlock.getId() != currentBlockId || currentBlock.getHeight() > blockchain.getHeight() + 1) {
                                    throw new WbxException.NotValidException("Database blocks in the wrong order!");
                                }
                                Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
                                List<TransactionImpl> validPhasedTransactions = new ArrayList<>();
                                List<TransactionImpl> invalidPhasedTransactions = new ArrayList<>();
                                validatePhasedTransactions(blockchain.getHeight(), validPhasedTransactions, invalidPhasedTransactions, duplicates);
                                if (validate && currentBlockId != Genesis.GENESIS_BLOCK_ID) {
                                    int curTime = Wbx.getEpochTime();
                                    validate(currentBlock, blockchain.getLastBlock(), curTime);
                                    byte[] blockBytes = currentBlock.bytes();
                                    JSONObject blockJSON = (JSONObject) JSONValue.parse(currentBlock.getJSONObject().toJSONString());
                                    if (!Arrays.equals(blockBytes, BlockImpl.parseBlock(blockJSON).bytes())) {
                                        throw new WbxException.NotValidException("Block JSON cannot be parsed back to the same block");
                                    }
                                    validateTransactions(currentBlock, blockchain.getLastBlock(), curTime, duplicates, true);
                                    for (TransactionImpl transaction : currentBlock.getTransactions()) {
                                        byte[] transactionBytes = transaction.bytes();
                                        if (currentBlock.getHeight() > Constants.NQT_BLOCK
                                                && !Arrays.equals(transactionBytes, TransactionImpl.newTransactionBuilder(transactionBytes).build().bytes())) {
                                            throw new WbxException.NotValidException("Transaction bytes cannot be parsed back to the same transaction: "
                                                    + transaction.getJSONObject().toJSONString());
                                        }
                                        JSONObject transactionJSON = (JSONObject) JSONValue.parse(transaction.getJSONObject().toJSONString());
                                        if (!Arrays.equals(transactionBytes, TransactionImpl.newTransactionBuilder(transactionJSON).build().bytes())) {
                                            throw new WbxException.NotValidException("Transaction JSON cannot be parsed back to the same transaction: "
                                                    + transaction.getJSONObject().toJSONString());
                                        }
                                    }
                                }
                                blockListeners.notify(currentBlock, Event.BEFORE_BLOCK_ACCEPT);
                                blockchain.setLastBlock(currentBlock);
                                accept(currentBlock, validPhasedTransactions, invalidPhasedTransactions, duplicates);
                                currentBlockId = currentBlock.getNextBlockId();
                                Db.db.clearCache();
                                Db.db.commitTransaction();
                                blockListeners.notify(currentBlock, Event.AFTER_BLOCK_ACCEPT);
                            } catch (WbxException | RuntimeException e) {
                                Db.db.rollbackTransaction();
                                Logger.logDebugMessage(e.toString(), e);
                                Logger.logDebugMessage("Applying block " + Long.toUnsignedString(currentBlockId) + " at height "
                                        + (currentBlock == null ? 0 : currentBlock.getHeight()) + " failed, deleting from database");
                                if (currentBlock != null) {
                                    currentBlock.loadTransactions();
                                    TransactionProcessorImpl.getInstance().processLater(currentBlock.getTransactions());
                                }
                                while (rs.next()) {
                                    try {
                                        currentBlock = BlockDb.loadBlock(con, rs, true);
                                        currentBlock.loadTransactions();
                                        TransactionProcessorImpl.getInstance().processLater(currentBlock.getTransactions());
                                    } catch (RuntimeException e2) {
                                        Logger.logErrorMessage(e2.toString(), e);
                                        break;
                                    }
                                }
                                BlockImpl lastBlock = BlockDb.deleteBlocksFrom(currentBlockId);
                                blockchain.setLastBlock(lastBlock);
                                popOffTo(lastBlock);
                                break outer;
                            }
                            blockListeners.notify(currentBlock, Event.BLOCK_SCANNED);
                            hasMore = true;
                        }
                        dbId = dbId + 1;
                    }
                }
                if (height == 0) {
                    for (DerivedDbTable table : derivedTables) {
                        table.createSearchIndex(con);
                    }
                }
                pstmtDone.executeUpdate();
                Db.db.commitTransaction();
                blockListeners.notify(currentBlock, Event.RESCAN_END);
                Logger.logMessage("...done at height " + blockchain.getHeight());
                if (height == 0 && validate) {
                    Logger.logMessage("SUCCESSFULLY PERFORMED FULL RESCAN WITH VALIDATION");
                }
                lastRestoreTime = 0;
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            } finally {
                isScanning = false;
            }
        } finally {
            blockchain.writeUnlock();
        }
    }
}
