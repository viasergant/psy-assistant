#!/usr/bin/env node

/**
 * Validates that all keys in en.json exist in uk.json and vice versa.
 * Exits with code 1 if there are missing keys.
 */

const fs = require('fs');
const path = require('path');

const I18N_DIR = path.join(__dirname, '../src/assets/i18n');
const EN_FILE = path.join(I18N_DIR, 'en.json');
const UK_FILE = path.join(I18N_DIR, 'uk.json');

/**
 * Recursively extracts all keys from a nested object in dot notation.
 * E.g., { auth: { login: { title: "..." } } } => ["auth.login.title"]
 */
function extractKeys(obj, prefix = '') {
  const keys = [];
  
  for (const key in obj) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    
    if (typeof obj[key] === 'object' && obj[key] !== null && !Array.isArray(obj[key])) {
      keys.push(...extractKeys(obj[key], fullKey));
    } else {
      keys.push(fullKey);
    }
  }
  
  return keys;
}

console.log('🔍 Validating i18n key parity...\n');

try {
  const enContent = fs.readFileSync(EN_FILE, 'utf-8');
  const ukContent = fs.readFileSync(UK_FILE, 'utf-8');
  
  const enJson = JSON.parse(enContent);
  const ukJson = JSON.parse(ukContent);
  
  const enKeys = extractKeys(enJson).sort();
  const ukKeys = extractKeys(ukJson).sort();
  
  const missingInUk = enKeys.filter(key => !ukKeys.includes(key));
  const missingInEn = ukKeys.filter(key => !enKeys.includes(key));
  
  let hasErrors = false;
  
  if (missingInUk.length > 0) {
    console.error(`❌ Missing in uk.json (${missingInUk.length} keys):`);
    missingInUk.forEach(key => console.error(`   - ${key}`));
    console.error('');
    hasErrors = true;
  }
  
  if (missingInEn.length > 0) {
    console.error(`❌ Missing in en.json (${missingInEn.length} keys):`);
    missingInEn.forEach(key => console.error(`   - ${key}`));
    console.error('');
    hasErrors = true;
  }
  
  if (!hasErrors) {
    console.log(`✅ All keys match (${enKeys.length} keys in both files)`);
    process.exit(0);
  } else {
    console.error('❌ Key parity validation failed');
    process.exit(1);
  }
  
} catch (error) {
  console.error('❌ Error reading or parsing files:');
  console.error(error.message);
  process.exit(1);
}
