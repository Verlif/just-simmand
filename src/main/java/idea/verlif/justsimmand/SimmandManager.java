package idea.verlif.justsimmand;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Verlif
 */
public class SimmandManager {

    private static final String SPLIT_PARAM = " ";
    private static final String TIP_UNKNOWN_COMMAND = "Unknown command";
    private static final String TIP_NO_SUCH_COMMAND = "No such command";

    /**
     * 指令表。<br/>
     * key - 指令名；<br/>
     * value - 指令参数序号
     */
    private final Map<String, Simmand> simmandMap;

    public SimmandManager() {
        simmandMap = new HashMap<>();
    }

    /**
     * 添加指令对象
     *
     * @param o 指令对象
     */
    public void add(Object o) {
        for (Method method : o.getClass().getDeclaredMethods()) {
            if ((method.getModifiers() & 1) > 0) {
                Simmand simmand = new Simmand();
                if (simmand.load(o, method)) {
                    for (String key : simmand.getKey()) {
                        simmandMap.put(key, simmand);
                    }
                }
            }
        }
    }

    /**
     * 通过指令行运行指令。<br/>
     * 请注意编译时参数名会被屏蔽。
     *
     * @param line 指令行
     * @return 指令结果
     */
    public String run(String line) {
        String[] ss = line.trim().split(SPLIT_PARAM, 2);
        // 判定参数
        switch (ss.length) {
            case 0:
                return TIP_UNKNOWN_COMMAND;
            case 1: {
                Simmand simmand = simmandMap.get(ss[0].trim());
                if (simmand == null) {
                    return TIP_NO_SUCH_COMMAND;
                }
                return simmand.run(null);
            }
            default: {
                Simmand simmand = simmandMap.get(ss[0].trim());
                if (simmand == null) {
                    return TIP_NO_SUCH_COMMAND;
                }
                return simmand.run(ss[1].trim());
            }
        }
    }

    public Set<String> allKey() {
        return simmandMap.keySet();
    }
}