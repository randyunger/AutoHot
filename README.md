# AutoHot

This is a demonstration of a lightweight in-memory caching pattern 
I learned at Gravity. This version is quite simple though. It uses Akka
for scheduling and ehCache for caching.

The idea is that simply by wrapping a call to a cache-worthy piece of 
code (like a DB call or API call or anything that adds latency) you 
can have that code execute on a background thread and refresh its value
in the cache. Then when you retrieve it you know you've already got a version
that's no older than the refresh time.

# Usage
There's currently only one method, heat. The first parameter list configures
the label (used as a key in the cash and for logging) and refresh frequency.
The second parameter list is a thunk (function that takes no parameters and
returns a value). The value is stored in the cache as well as returned from
the call to heat.

Due to the magic of closures, the calling context remains in scope on
future calls 

    def getConfigurationFromDB(dbService: DbService) = {
    
        dbService.oneTimeConfiguration(some, parameters)
    
        AutoHot.heat("example", 5.minutes){
            dbService.fetch(configurationTable)
        }
    }

The above example will allow you anywhere you can call getConfigurationFromDB
to always have values that are no more than 5 minutes old. This is great
for data that doesn't change too frequently, and when there are only a few unique 
 queries (that's actually a lot of cases!)
 
 Don't use it like you would an LRU cache, because it doesn't automatically
 evict older entries when it fills up. Best used for long running, one-off queries
 such as configuration values.