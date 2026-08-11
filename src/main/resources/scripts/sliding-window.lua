local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local clear_before = now - (window * 1000)

-- Remove all entries older than (now - window) from the sorted set
redis.call('ZREMRANGEBYSCORE', key, 0, clear_before)

-- Count remaining entries
local count = redis.call('ZCARD', key)

-- If count < limit, add current timestamp + random suffix and return count + 1
if count < limit then
    local member = now .. '_' .. math.random(1, 1000000)
    redis.call('ZADD', key, now, member)
    redis.call('EXPIRE', key, window)
    return count + 1
else
    return -1
end
