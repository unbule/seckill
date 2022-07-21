-- 资源唯一标志位
local key = KEYS[1]
--限流大小
local limit = tonumber(ARGV[1])

--获取当前流量
local currentLimit = tonumber(redis.call('get', key) or "0")

if currentLimit + 1 > limit then
    return 0;
else

    redis.call("INCRBY", key, 1)

    if currentLimit == 0 then
        redis.call("EXPIRE", key, 2)
    end

    return currentLimit + 1
end