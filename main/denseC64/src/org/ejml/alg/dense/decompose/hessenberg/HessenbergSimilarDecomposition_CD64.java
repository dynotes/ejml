/*
 * Copyright (c) 2009-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.alg.dense.decompose.hessenberg;

import org.ejml.alg.dense.decompose.UtilDecompositons_CD64;
import org.ejml.alg.dense.decompose.qr.QrHelperFunctions_CD64;
import org.ejml.data.CDenseMatrix64F;
import org.ejml.data.Complex64F;
import org.ejml.interfaces.decomposition.DecompositionInterface;

import java.util.Arrays;

/**
 * <p>
 * Complex Hessenberg decomposition.  It find matrices O and P such that:<br>
 * <br>
 * A = OPO<sup>H</sup><br>
 * <br>
 * where A is an m by m matrix, O is an orthogonal matrix, and P is an upper Hessenberg matrix.
 * </p>
 *
 * <p>
 * A matrix is upper Hessenberg if a<sup>ij</sup> = 0 for all i > j+1. For example, the following matrix
 * is upper Hessenberg.<br>
 * <br>
 * WRITE IT OUT USING A TABLE
 * </p>
 *
 * <p>
 * This decomposition is primarily used as a step for computing the eigenvalue decomposition of a matrix.
 * The basic algorithm comes from David S. Watkins, "Fundamentals of MatrixComputations" Second Edition.
 * </p>
 */
// TODO create a column based one similar to what was done for QR decomposition?
public class HessenbergSimilarDecomposition_CD64
        implements DecompositionInterface<CDenseMatrix64F> {
    // A combined matrix that stores te upper Hessenberg matrix and the orthogonal matrix.
    private CDenseMatrix64F QH;
    // number of rows and columns of the matrix being decompose
    private int N;

    // the first element in the orthogonal vectors
    private double gammas[];
    // temporary storage
    private double b[];
    private double u[];
    private Complex64F tau = new Complex64F();
    /**
     * Creates a decomposition that won't need to allocate new memory if it is passed matrices up to
     * the specified size.
     *
     * @param initialSize Expected size of the matrices it will decompose.
     */
    public HessenbergSimilarDecomposition_CD64(int initialSize) {
        gammas = new double[ initialSize ];
        b = new double[ initialSize*2 ];
        u = new double[ initialSize*2 ];
    }

    public HessenbergSimilarDecomposition_CD64() {
        this(5);
    }

    /**
     * Computes the decomposition of the provided matrix.  If no errors are detected then true is returned,
     * false otherwise.
     * @param A  The matrix that is being decomposed.  Not modified.
     * @return If it detects any errors or not.
     */
    @Override
    public boolean decompose( CDenseMatrix64F A )
    {
        if( A.numRows != A.numCols )
            throw new IllegalArgumentException("A must be square.");
        if( A.numRows <= 0 )
            return false;

        QH = A;

        N = A.numCols;

        if( b.length < N*2 ) {
            b = new double[ N*2 ];
            gammas = new double[ N ];
            u = new double[ N*2 ];
        }
        return _decompose();
    }

    @Override
    public boolean inputModified() {
        return true;
    }

    /**
     * The raw QH matrix that is stored internally.
     *
     * @return QH matrix.
     */
    public CDenseMatrix64F getQH() {
        return QH;
    }

    /**
     * An upper Hessenberg matrix from the decomposition.
     *
     * @param H If not null then the results will be stored here.  Otherwise a new matrix will be created.
     * @return The extracted H matrix.
     */
    public CDenseMatrix64F getH( CDenseMatrix64F H ) {
        H = UtilDecompositons_CD64.checkZeros(H,N,N);

        // copy the first row
        System.arraycopy(QH.data, 0, H.data, 0, N*2);

        for( int i = 1; i < N; i++ ) {
            System.arraycopy(QH.data, (i*N+i-1)*2, H.data, (i*N+i-1)*2, (N-i+1)*2);
        }

        return H;
    }

    /**
     * An orthogonal matrix that has the following property: H = Q<sup>T</sup>AQ
     *
     * @param Q If not null then the results will be stored here.  Otherwise a new matrix will be created.
     * @return The extracted Q matrix.
     */
    public CDenseMatrix64F getQ( CDenseMatrix64F Q ) {
        Q = UtilDecompositons_CD64.checkIdentity(Q,N,N);

        Arrays.fill(u,0,N*2,0);
        for( int j = N-2; j >= 0; j-- ) {
            QrHelperFunctions_CD64.extractHouseholderColumn(QH,j+1,N,j,u,0);
            QrHelperFunctions_CD64.rank1UpdateMultR(Q, u, 0,gammas[j], j + 1, j + 1, N, b);
        }

        return Q;
    }

    /**
     * Internal function for computing the decomposition.
     */
    private boolean _decompose() {
        double h[] = QH.data;

        for( int k = 0; k < N-2; k++ ) { // k = column
            u[k*2] = 0;
            u[k*2+1] = 0;
            double max = QrHelperFunctions_CD64.extractColumnAndMax(QH,k+1,N,k,u,0);

            if( max > 0 ) {
                // -------- set up the reflector Q_k

                double gamma = QrHelperFunctions_CD64.computeTauGammaAndDivide(k+1,N,u,max,tau);
                gammas[k] = gamma;

                // divide u by u_0
                double real_u_0 = u[(k+1)*2]   + tau.real;
                double imag_u_0 = u[(k+1)*2+1] + tau.imaginary;
                QrHelperFunctions_CD64.divideElements(k + 2, N, u, 0, real_u_0,imag_u_0 );

                // write the reflector into the lower left column of the matrix
                for (int i = k+2; i < N; i++) {
                    h[(i*N+k)*2]   = u[i*2];
                    h[(i*N+k)*2+1] = u[i*2+1];
                }

                u[(k+1)*2]   = 1;
                u[(k+1)*2+1] = 0;

                // ---------- multiply on the left by Q_k
                QrHelperFunctions_CD64.rank1UpdateMultR(QH, u,0, gamma, k + 1, k + 1, N, b);

                // ---------- multiply on the right by Q_k
                QrHelperFunctions_CD64.rank1UpdateMultL(QH, u,0, gamma, 0, k + 1, N);

                // since the first element in the householder vector is known to be 1
                // store the full upper hessenberg
                h[((k+1)*N+k)*2]   = -tau.real*max;
                h[((k+1)*N+k)*2+1] = -tau.imaginary*max;

            } else {
                gammas[k] = 0;
            }

        }

        return true;
    }

    public double[] getGammas() {
        return gammas;
    }
}
