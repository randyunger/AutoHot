# AutoHot

This is a demonstration of a lightweight in-memory caching pattern 
I learned at Gravity.

The idea is that simply by wrapping a call to a cache-worthy piece of 
code (like a DB call or API call or anything that adds latency) you 
can have that code execute on a background thread and refresh its value
in the cache. Then when you retrieve it you know you've already got a version
that's no older than the refresh time.

# Usage