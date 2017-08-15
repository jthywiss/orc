
package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import orc.run.porce.runtime.Counter;

@NodeChild("counter")
public class NewToken extends Expression {
    @Specialization
    public PorcEUnit run(final Counter counter) {
        counter.newToken();
        return PorcEUnit.SINGLETON;
    }

    public static NewToken create(final Expression parent) {
        return NewTokenNodeGen.create(parent);
    }
}
