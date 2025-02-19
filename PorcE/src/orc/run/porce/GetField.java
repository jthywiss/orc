//
// GetField.java -- Truffle node GetField
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.Accessor;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.PorcEExecution;
import orc.values.Field;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeChild(value = "object", type = Expression.class)
@Introspectable
@ImportStatic({ SpecializationConfiguration.class })
public abstract class GetField extends Expression {
    protected final Field field;
    protected final PorcEExecution execution;

    protected GetField(final Field field, final PorcEExecution execution) {
        this.field = field;
        this.execution = execution;
    }

    @Specialization(guards = { "canGetNode.executeCanGet(frame, accessor, obj)" },
            limit = "GetFieldMaxCacheSize")
    public Object cachedAccessor(final VirtualFrame frame, final Object obj,
            @Cached("create()") AccessorCanGet canGetNode,
            @Cached("create()") AccessorGet getNode,
            @Cached("getAccessorWithBoundary(obj)") final Accessor accessor) {
        try {
            return getNode.executeGet(frame, accessor, obj);
        } catch (final Exception e) {
            CompilerDirectives.transferToInterpreter();
            execution.notifyOfException(e, this);
            throw new HaltException();
        }
    }

    @Specialization(replaces = { "cachedAccessor" })
    public Object slowPath(final Object obj) {
        try {
            final Accessor accessor = getAccessorWithBoundary(obj);
            return accessWithBoundary(accessor, obj);
        } catch (final Exception e) {
            CompilerDirectives.transferToInterpreter();
            execution.notifyOfException(e, this);
            throw new HaltException();
        }
    }

    @TruffleBoundary
    protected Accessor getAccessorWithBoundary(final Object t) {
        return execution.runtime().getAccessor(t, field);
    }

    @TruffleBoundary(allowInlining = true)
    static Object accessWithBoundary(final Accessor accessor, final Object obj) {
        return accessor.get(obj);
    }

    @TruffleBoundary(allowInlining = true)
    static boolean canGetWithBoundary(final Accessor accessor, final Object obj) {
        return accessor.canGet(obj);
    }

    public static GetField create(final Expression object, final Field field, final PorcEExecution execution) {
        return GetFieldNodeGen.create(field, execution, object);
    }
}
