package org.mytang.fairydust.core.util;

import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;
import com.ql.util.express.instruction.op.OperatorIn;
import java.util.ArrayList;
import java.util.List;

/**
 * @author tangmengyang
 */
public final class ExpressUtils {

    private static final ExpressRunner EXPRESS_RUNNER = new ExpressRunner();

    private static final ThreadLocal<List<String>> ERROR_LIST = ThreadLocal.withInitial(ArrayList::new);

    public static boolean getBoolean(String condition, IExpressContext<String, Object> context) {
        Object result = evaluate(condition, context);
        if (result instanceof Boolean) {
            return (boolean) result;
        } else if (result instanceof String) {
            return Boolean.valueOf((String) result);
        } else {
            return false;
        }
    }

    public static Object evaluate(String expressString, IExpressContext<String, Object> context) {
        List<String> currentErrorList = ERROR_LIST.get();
        try {
            Object result = EXPRESS_RUNNER.execute(expressString, context, currentErrorList, true, false);
            if (currentErrorList != null && !currentErrorList.isEmpty()) {
                StringBuilder errorBuilder = new StringBuilder();
                for (String error : currentErrorList) {
                    errorBuilder.append(error).append("#");
                }
                throw new RuntimeException(errorBuilder.toString());
            }
            return result;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            if (currentErrorList != null) {
                currentErrorList.clear();
            }
        }
    }

    public static boolean isValid(String expressString) {
        try {
            EXPRESS_RUNNER.parseInstructionSet(expressString);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static void clearCache() {
        EXPRESS_RUNNER.clearExpressCache();
    }

    static {
        if (!EXPRESS_RUNNER.isShortCircuit()) {
            EXPRESS_RUNNER.setShortCircuit(true);
        }
        try {
            //增加类似于SQL的 not in 操作
            EXPRESS_RUNNER.addOperator("nin", new OperatorIn("nin") {
                private static final long serialVersionUID = 9063776438453764211L;

                @Override
                public Object executeInner(Object[] list) throws Exception {
                    if (list[0] == null) {
                        return true;
                    }
                    return !((Boolean) super.executeInner(list));
                }
            });
            //别名
            EXPRESS_RUNNER.addOperatorWithAlias("notin", "nin", null);
            EXPRESS_RUNNER.addOperatorWithAlias("notIn", "nin", null);
            EXPRESS_RUNNER.addOperatorWithAlias("NOTIN", "nin", null);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
