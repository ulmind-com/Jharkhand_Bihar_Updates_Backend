const { createClient } = require('redis');

async function run() {
  const client = createClient({
    url: 'redis://default:c78A7qI9ZVhh4Uwt1rQAAeHC9vuzscnQ@redis-14833.crce263.ap-south-1-1.ec2.cloud.redislabs.com:14833'
  });

  client.on('error', err => console.log('Redis Client Error', err));

  await client.connect();
  console.log("Connected to Redis");
  await client.flushAll();
  console.log("Flushed Redis cache.");
  await client.disconnect();
}
run();
