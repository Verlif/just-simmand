package idea.verlif.justsimmand;

import idea.verlif.justsimmand.anno.SmdClass;
import idea.verlif.justsimmand.anno.SmdOption;
import idea.verlif.justsimmand.anno.SmdParam;
import idea.verlif.justsimmand.info.SmdGroupInfo;
import idea.verlif.justsimmand.info.SmdMethodInfo;
import idea.verlif.parser.ParamParserService;
import idea.verlif.parser.cmdline.ArgParser;
import idea.verlif.parser.cmdline.ArgValues;
import idea.verlif.reflection.util.MethodUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 指令执行器，用于执行指令行或指令参数。
 *
 * @author Verlif
 */
public class SmdExecutor extends ParamParserService {

    private static final String SPLIT_PARAM = " ";
    private static final String KEY_PARAM_PREFIX = "--";

    /**
     * 指令表。<br/>
     * key - 指令名；<br/>
     * value - 指令参数序号
     */
    private final Map<String, Simmand> simmandMap;

    /**
     * 指令信息表。
     */
    private final Map<String, SmdGroupInfo> smdInfoMap;

    /**
     * 前缀替换表
     */
    private final Map<String, String> prefixMap;

    /**
     * 默认指令配置
     */
    private LoadConfig defaultConfig;

    /**
     * 指令配置
     */
    private SmdConfig smdConfig;

    /**
     * 指令对象工厂类
     */
    private SimmandFactory simmandFactory;

    /**
     * 参数解析器工厂类
     */
    private ArgParserFactory argParserFactory;

    public SmdExecutor() {
        simmandMap = new HashMap<>();
        smdInfoMap = new HashMap<>();
        defaultConfig = new LoadConfig();
        prefixMap = new HashMap<>();

        add(new HelpSmd());
        addPrefixReplace("help help", "help");
    }

    public void setSmdConfig(SmdConfig smdConfig) {
        this.smdConfig = smdConfig;
    }

    public void setSimmandFactory(SimmandFactory simmandFactory) {
        this.simmandFactory = simmandFactory;
    }

    public void setArgParserFactory(ArgParserFactory argParserFactory) {
        this.argParserFactory = argParserFactory;
    }

    /**
     * 添加指令对象。使用默认的参数解析器与配置。
     *
     * @param o 指令对象
     */
    public void add(Object o) {
        add(o, defaultConfig);
    }

    /**
     * 添加指令对象。
     *
     * @param o      指令对象
     * @param config 指定加载的方法
     */
    public void add(Object o, LoadConfig config) {
        if (simmandFactory == null) {
            simmandFactory = new SpecialSimmandFactory();
        }
        if (smdConfig == null) {
            smdConfig = new SmdConfig();
        }
        Class<?> cla = o.getClass();
        // 新增指令信息
        SmdGroupInfo smdGroupInfo = new SmdGroupInfo();
        SmdClass smdClass = cla.getAnnotation(SmdClass.class);
        // 构造指令key
        String name;
        if (smdClass == null || smdClass.value().length() == 0) {
            name = cla.getSimpleName();
        } else {
            name = smdClass.value();
        }
        // 构建指令信息
        smdGroupInfo.setKey(name);
        if (smdClass != null) {
            smdGroupInfo.setDescription(smdClass.description());
        }
        List<Method> allMethods = MethodUtil.getAllMethods(cla);
        List<SmdMethodInfo> smdMethodInfoList = new ArrayList<>();
        for (Method method : allMethods) {
            // 过滤方法
            if (config.isAllowedModifier(method.getModifiers()) && config.isAllowedMethod(method.getName())) {
                // 实例化指令
                Simmand simmand = simmandFactory.create(config);
                if (simmand.load(o, method)) {
                    smdMethodInfoList.add(simmand.getMethodInfo());
                    for (String key : simmand.getKey()) {
                        if (smdConfig.isClassNameGroup()) {
                            key = name + " " + key;
                        }
                        if (config.isAddWithReplace() || !simmandMap.containsKey(key)) {
                            simmandMap.put(key, simmand);
                        }
                    }
                }
            }
        }
        // 添加指令信息到信息表
        if (!smdMethodInfoList.isEmpty()) {
            smdGroupInfo.addSmdMethodInfo(smdMethodInfoList);
            smdInfoMap.put(smdGroupInfo.getKey(), smdGroupInfo);
        }
    }

    /**
     * 通过指令行运行指令。<br/>
     * 请注意编译时参数名会被屏蔽。
     *
     * @param line 指令行
     * @return 指令结果。执行成功则返回执行方法的返回对象，否则返回错误信息（String）。
     */
    public Object run(String line) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // 处理指令别名
        String newLine = line;
        for (Map.Entry<String, String> entry : prefixMap.entrySet()) {
            if (line.startsWith(entry.getKey())) {
                newLine = entry.getValue() + line.substring(entry.getKey().length());
                break;
            }
        }
        // 构建指令参数
        String[] ss = newLine.split(SPLIT_PARAM, 3);
        String group = null;
        String methodName;
        String paramLine = "";
        if (ss.length == 1) {
            methodName = ss[0];
        } else if (ss.length == 2) {
            if (smdConfig.isClassNameGroup()) {
                group = ss[0];
                methodName = ss[1];
            } else {
                methodName = ss[0];
                paramLine = ss[1];
            }
        } else {
            if (smdConfig.isClassNameGroup()) {
                group = ss[0];
                methodName = ss[1];
                paramLine = ss[2];
            } else {
                methodName = ss[0];
                paramLine = ss[1] + SPLIT_PARAM + ss[2];
            }
        }
        String key = group == null ? methodName : group + SPLIT_PARAM + methodName;
        Simmand simmand = simmandMap.get(key);
        if (simmand == null) {
            throw new NoSuchMethodException();
        }
        if (argParserFactory == null) {
            argParserFactory = new SpecialArgParser();
        }
        ArgParser argParser = argParserFactory.create(group, methodName);
        ArgValues argValues = argParser.parseLine(paramLine);
        return simmand.run(argValues);
    }

    /**
     * 获取所有可用的指令Key
     *
     * @return 指令Key集
     */
    public Set<String> allKey() {
        return simmandMap.keySet();
    }

    /**
     * 添加指令前缀替换
     *
     * @param name    指令前缀
     * @param aliases 指令替换字符
     */
    public void addPrefixReplace(String name, String... aliases) {
        for (String alias : aliases) {
            prefixMap.put(alias, name);
        }
    }

    /**
     * 获取指令本名
     *
     * @param alias 指令别名
     * @return 指令本名
     */
    public String getOriginalName(String alias) {
        return prefixMap.get(alias);
    }

    /**
     * 设置默认的指令参数解析器，用于 {@link #add(Object)}、{@link #add(Object, LoadConfig)} 方法中。
     *
     * @param defaultConfig 参数解析器
     */
    public void setDefaultConfig(LoadConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    /**
     * 获取默认配置。
     *
     * @return 默认的指令加载配置。
     */
    public LoadConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * 特殊指令对象
     */
    private class SpecialSimmand extends Simmand {

        protected SpecialSimmand(LoadConfig config) {
            super(config);
        }

        @Override
        protected Object convert(String str, Class<?> cla) {
            return SmdExecutor.this.parse(cla, str);
        }
    }

    /**
     * 特殊指令对象工厂类
     */
    private class SpecialSimmandFactory implements SimmandFactory {

        @Override
        public Simmand create(LoadConfig config) {
            return new SpecialSimmand(config);
        }
    }

    /**
     * 特殊指令参数解析器工厂类
     */
    private static class SpecialArgParser implements ArgParserFactory {

        @Override
        public ArgParser create(String group, String methodName) {
            return new SmdArgParser(KEY_PARAM_PREFIX);
        }
    }

    @SmdClass(value = "help", description = "帮助指令，用于查询当前的指令信息")
    private class HelpSmd {

        @SmdOption(value = "help", description = "查看当前加载的指令列表",
                example = "输入 help 来显示所有的指令，输入 help --key name 来显示name指令的信息")
        public List<SmdGroupInfo> help(
                @SmdParam(value = "group", force = false, description = "指定指令指令组名") String group,
                @SmdParam(value = "key", force = false, description = "指定指令名") String key) {
            Stream<SmdGroupInfo> stream = smdInfoMap.values().stream();
            List<SmdGroupInfo> infoList;
            if (group != null) {
                infoList = stream.filter(smdGroupInfo -> smdGroupInfo.getKey().startsWith(group)).collect(Collectors.toList());
            } else {
                infoList = stream.collect(Collectors.toList());
            }
            if (key != null) {
                for (SmdGroupInfo smdGroupInfo : infoList) {
                    smdGroupInfo.getMethodInfoList().removeIf(info -> {
                        for (String k : info.getKey()) {
                            if (k.equals(key)) {
                                return false;
                            }
                        }
                        return true;
                    });
                }
            }
            return infoList;
        }
    }
}