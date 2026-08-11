local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])

-- Retrieve stored tokens and last refill timestamp
local data = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(data[1])
local last_refill = tonumber(data[2])

if not tokens then
    -- First request, initialize bucket to full capacity minus 1 (since we consume 1 now)
    tokens = capacity - 1
    last_refill = now
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
    redis.call('EXPIRE', key, ttl)
    return math.floor(tokens)
else
    -- Calculate tokens to add based on elapsed time and refill rate
    local elapsed = (now - last_refill) / 1000.0 -- in seconds
    local tokens_to_add = elapsed * refill_rate
    tokens = tokens + tokens_to_add
    
    -- Cap tokens at bucket capacity (limit)
    if tokens > capacity then
        tokens = capacity
    end
    
    -- If tokens >= 1, consume one token and return remaining tokens
    if tokens >= 1 then
        tokens = tokens - 1
        redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
        redis.call('EXPIRE', key, ttl)
        return math.floor(tokens)
    else
        -- If tokens < 1, reject and return -1
        return -1
    end
end
