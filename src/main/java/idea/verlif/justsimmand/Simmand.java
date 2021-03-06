package idea.verlif.justsimmand;

import idea.verlif.justsimmand.anno.SimmOption;
import idea.verlif.justsimmand.anno.SimmParam;
import idea.verlif.parser.ParamParser;
import idea.verlif.parser.ParamParserService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指令对象，由{@link SimmandManager}自动生成。
 *
 * @author Verlif
 */
public class Simmand {

    private static final String TAG_PARAM = "--";
    private static final String SPLIT_PARAM = " ";

    private final ParamParserService pps;
    private final SimmandConfig config;

    /**
     * 指令Key
     */
    private String[] key;

    /**
     * 指令对象
     */
    private Object object;

    /**
     * 指令方法
     */
    private Method method;

    /**
     * 解析表。<br/>
     * key - 指令参数序号；<br/>
     * value - 对应的参数解析
     */
    private Map<Integer, ParamParser<?>> parserMap;

    /**
     * 参数名序号表。<br/>
     * key - 参数名；<br/>
     * value - 指令参数序号
     */
    private Map<String, Integer> paramMap;

    /**
     * 参数值默认值表。<br/>
     * key - 指令参数序号；<br/>
     * value - 参数默认值
     */
    private Map<Integer, Object> valueMap;

    protected Simmand(ParamParserService service, SimmandConfig config) {
        pps = service;
        this.config = config;
    }

    /**
     * 加载方法到此指令
     *
     * @param o      指令的所属对象
     * @param method 指令方法
     */
    public boolean load(Object o, Method method) {
        SimmOption simmOption = method.getAnnotation(SimmOption.class);
        if (config.getLoadMode() == SimmandConfig.LoadMode.POSITIVE && simmOption != null && !simmOption.isCommand()
                || config.getLoadMode() == SimmandConfig.LoadMode.NEGATIVE && simmOption == null) {
            return false;
        }
        this.object = o;
        this.method = method;
        this.method.setAccessible(true);
        // 设置指令名
        if (simmOption == null || simmOption.value().length == 0) {
            key = new String[]{method.getName()};
        } else {
            key = simmOption.value();
        }
        // 设置指令参数解析表
        Class<?>[] paramTypes = method.getParameterTypes();
        parserMap = new HashMap<>(paramTypes.length);
        for (int i = 0, size = paramTypes.length; i < size; i++) {
            ParamParser<?> pp = pps.getParser(paramTypes[i]);
            if (pp == null) {
                pp = pps.getParser(String.class);
            }
            parserMap.put(i, pp);
        }
        // 设置参数默认值与参数名序号表
        valueMap = new HashMap<>(paramTypes.length);
        paramMap = new HashMap<>(paramTypes.length);
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String param = parameter.getName();
            SimmParam simmParam = parameter.getAnnotation(SimmParam.class);
            if (simmParam != null) {
                valueMap.put(i, parserMap.get(i).parser(simmParam.defaultVal()));
                if (simmParam.key().length() > 0) {
                    param = simmParam.key();
                }
            } else {
                valueMap.put(i, null);
            }
            paramMap.put(param, i);
        }
        return true;
    }

    /**
     * 获取此指令对象的所有方法key。
     *
     * @return 此指令对象的所有加载的方法key，这些key都可以被识别。
     */
    public String[] getKey() {
        return key;
    }

    /**
     * 指令执行
     *
     * @return 指令返回值
     */
    public Object run(String line) throws InvocationTargetException, IllegalAccessException {
        if (method == null) {
            return "No such command!!!";
        }
        // 参数值表
        int size = valueMap.size();
        Map<Integer, Object> valueMap = new HashMap<>(size);
        try {
            if (line != null) {
                String[] paramSplit = split(line);
                // 给定参数名的临时序号
                Integer i = null;
                // 未给定参数名的序号
                int no = 0;
                // 填充参数值
                for (String s : paramSplit) {
                    String param = s.trim();
                    if (param.length() == 0) {
                        continue;
                    }
                    // 设定参数
                    if (param.startsWith(TAG_PARAM)) {
                        i = paramMap.get(param);
                    } else if (i == null) {
                        if (no < size) {
                            valueMap.put(no, parserMap.get(no).parser(param));
                            no++;
                        }
                    } else if (i < size) {
                        valueMap.put(i, parserMap.get(i).parser(param));
                        i = null;
                    }
                }
            }
        } catch (Exception e) {
            return "Something error: " + e.getMessage();
        }
        // 填充默认值
        for (Integer index : this.valueMap.keySet()) {
            valueMap.putIfAbsent(index, this.valueMap.get(index));
        }
        // 判断值是否缺失
        Object[] objects = new Object[size];
        for (int j = 0; j < size; j++) {
            Object o = valueMap.get(j);
            if (o == null) {
                return "Lack of param - index : " + (j + 1);
            }
            objects[j] = o;
        }
        return method.invoke(object, objects);
    }

    private String[] split(String str) {
        char[] chars = str.toCharArray();
        boolean in = false, ready = false, tran = false;
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if (in) {
                if (c == '\"') {
                    in = false;
                    list.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                    ready = true;
                }
            } else {
                if (c == '\"') {
                    if (tran) {
                        sb.append(c);
                        tran = false;
                    } else {
                        in = true;
                        if (ready) {
                            list.add(sb.toString());
                            sb.setLength(0);
                        }
                    }
                } else if (c == '\\') {
                    tran = true;
                } else if (c == ' ') {
                    if (sb.length() > 0) {
                        list.add(sb.toString());
                    }
                    ready = false;
                    sb.setLength(0);
                } else {
                    sb.append(c);
                    ready = true;
                }
            }
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list.toArray(new String[0]);
    }
}
