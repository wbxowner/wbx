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

//import wbx.util.Logger;

public interface Fee {

    long getFee(TransactionImpl transaction, Appendix appendage);

    Fee DEFAULT_FEE = new Fee.ConstantFee(Constants.ONE_WIN);
    //Fee DEFAULT_FEE = new Fee.ConstantFee(Constants.TRANSACTION_FEE);
    //long transactionFee = new Long((Wbx.getIntProperty("wbx.transactionFee", (int) Constants.ONE_WIN))/Wbx.getIntProperty("wbx.reductorFee", 1));
    //long transactionFee = new Long(Wbx.getIntProperty("wbx.transactionFee", (int) Constants.ONE_WIN));
    //Fee DEFAULT_FEE = new Fee.ConstantFee(transactionFee);
    //Fee DEFAULT_FEE = new Fee.ConstantFee(transactionFee);

    Fee NONE = new Fee.ConstantFee(0L);

    final class ConstantFee implements Fee {

        private final long fee;

        public ConstantFee(long fee) {
            this.fee = fee;
        }

        @Override
        public long getFee(TransactionImpl transaction, Appendix appendage) {
        	//return fee/Constants.REDUCTOR_FEE < Constants.ONE_WIN/Constants.REDUCTOR_FEE ? Constants.ONE_WIN/Constants.REDUCTOR_FEE : fee/Constants.REDUCTOR_FEE;
            return fee/Constants.REDUCTOR_FEE;
        }

    }

    abstract class SizeBasedFee implements Fee {

        private final long constantFee;
        private final long feePerSize;
        private final int unitSize;

        public SizeBasedFee(long feePerSize) {
            this(0, feePerSize);
        }

        public SizeBasedFee(long constantFee, long feePerSize) {
            this(constantFee, feePerSize, 1024);
        }

        public SizeBasedFee(long constantFee, long feePerSize, int unitSize) {
            int height = Wbx.getBlockchain().getHeight();
            if (height < Constants.NEW_FEE_CALCULATION_BLOCK) {
                this.constantFee = constantFee/Constants.REDUCTOR_FEE < Constants.ONE_WIN/Constants.REDUCTOR_FEE ? Constants.ONE_WIN/Constants.REDUCTOR_FEE : constantFee/Constants.REDUCTOR_FEE;
                this.feePerSize = feePerSize;
                this.unitSize = unitSize;
            } else {
                //Logger.logInfoMessage(String.format("1 SizeBasedFee constantFee=%d feePerSize=%d unitSize=%d .", ((long) constantFee), ((long) feePerSize), unitSize));
                if (constantFee == 0) this.constantFee = constantFee;
                else this.constantFee = constantFee/Constants.REDUCTOR_FEE < Constants.ONE_WIN/Constants.REDUCTOR_FEE ? Constants.ONE_WIN/Constants.REDUCTOR_FEE : constantFee/Constants.REDUCTOR_FEE;
                if (feePerSize == 0) this.feePerSize = feePerSize;
                else this.feePerSize = feePerSize/Constants.REDUCTOR_FEE < Constants.ONE_WIN/Constants.REDUCTOR_FEE ? Constants.ONE_WIN/Constants.REDUCTOR_FEE : feePerSize/Constants.REDUCTOR_FEE;
                //this.feePerSize = feePerSize;
                this.unitSize = unitSize;
                //Logger.logInfoMessage(String.format("2 SizeBasedFee this.constantFee=%d this.feePerSize=%d this.unitSize=%d .", ((long) this.constantFee), ((long) this.feePerSize), this.unitSize));
            }
        }

        // the first size unit is free if constantFee is 0
        @Override
        public final long getFee(TransactionImpl transaction, Appendix appendage) {
            int height = Wbx.getBlockchain().getHeight();
            int size = getSize(transaction, appendage) - 1;
            //Logger.logInfoMessage(String.format("1 getFee size=%d feePerSize=%d unitSize=%d .", size, ((long) feePerSize), unitSize));
            if (size < 0) {
                return constantFee;
            }
            //Logger.logInfoMessage(String.format("2 getFee constantFee=%d size=%d feePerSize=%d unitSize=%d .", ((long) constantFee), size, ((long) feePerSize), unitSize));
            //long fee = (Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize))/Constants.REDUCTOR_FEE);
            //Logger.logInfoMessage(String.format("3 getFee fee=%d .", ((long) fee)));
            if (height < Constants.NEW_FEE_CALCULATION_BLOCK) {
                long fee = (Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize))/Constants.REDUCTOR_FEE);
                return fee < Constants.ONE_WIN/Constants.REDUCTOR_FEE ? Constants.ONE_WIN/Constants.REDUCTOR_FEE : fee;
            }
            long fee = (Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize)));
            if (fee == 0) fee = fee;
            else fee = fee < Constants.ONE_WIN/Constants.REDUCTOR_FEE ? Constants.ONE_WIN/Constants.REDUCTOR_FEE : fee;
            //Logger.logInfoMessage(String.format("4 getFee fee=%d .", ((long) fee)));
            return fee;
            //return (Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize))/Constants.REDUCTOR_FEE);
            //return (Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize))/Wbx.getIntProperty("wbx.reductorFee", 1));
            //return Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize));
        }

        public abstract int getSize(TransactionImpl transaction, Appendix appendage);

    }

}
