const { Client } = require('pg');
const client = new Client({ connectionString: 'postgresql://gen_user:%3D%2B5E.S_l70xpHd@5.181.182.189:5432/default_db' });

async function run() {
  await client.connect();
  try {
    console.log("Deleting properties and related data...");
    await client.query('DELETE FROM property_inquiries;');
    await client.query('DELETE FROM property_images;');
    await client.query('DELETE FROM property_floor_plans;');
    await client.query('DELETE FROM property_amenities;');
    await client.query('DELETE FROM properties;');
    console.log("Deleted properties.");

    console.log("Deleting specific vendors...");
    const vendorIds = await client.query(`SELECT id FROM vendors WHERE shop_name IN ('आNARDAना', 'Ambur Delight Biriyani')`);
    const ids = vendorIds.rows.map(r => r.id);
    if (ids.length > 0) {
      const idStr = ids.join(',');
      const products = await client.query(`SELECT id FROM products WHERE vendor_id IN (${idStr})`);
      const prodIds = products.rows.map(r => r.id);
      if (prodIds.length > 0) {
        const prodStr = prodIds.join(',');
        await client.query(`DELETE FROM cart_items WHERE product_id IN (${prodStr})`);
      }
      
      // Delete cart_items associated with the vendor's cart_sessions
      await client.query(`DELETE FROM cart_items WHERE cart_session_id IN (SELECT id FROM cart_sessions WHERE vendor_id IN (${idStr}))`);
      // Delete cart_sessions
      await client.query(`DELETE FROM cart_sessions WHERE vendor_id IN (${idStr})`);
      
      await client.query(`DELETE FROM products WHERE vendor_id IN (${idStr})`);
      await client.query(`DELETE FROM vendors WHERE id IN (${idStr})`);
      console.log("Deleted specific vendors and related data.");
    } else {
        console.log("Vendors not found or already deleted.");
    }
    
    const remaining = await client.query('SELECT shop_name FROM vendors');
    console.log("Remaining vendors:", remaining.rows);
  } catch (err) {
    console.error(err);
  } finally {
    await client.end();
  }
}
run();
