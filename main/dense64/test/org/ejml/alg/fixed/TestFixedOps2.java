/*
 * Copyright (c) 2009-2015, Peter Abeles. All Rights Reserved.
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

package org.ejml.alg.fixed;

import org.ejml.data.FixedMatrix2_64F;
import org.ejml.data.FixedMatrix2x2_64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFixedOps2 extends CompareFixedToCommonOps {

    public TestFixedOps2() {
        super(FixedOps2.class);
    }

    @Test
    public void diag() {
        FixedMatrix2x2_64F m = new FixedMatrix2x2_64F(1,2,3,4);
        FixedMatrix2_64F found = new FixedMatrix2_64F();

        FixedOps2.diag(m,found);

        assertEquals(1,found.a1,1e-8);
        assertEquals(4,found.a2,1e-8);
    }
}
