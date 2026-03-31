#!/usr/bin/env node

/**
 * Validates that Ukrainian translations are not excessively longer than English.
 * WCAG 2.1 AA guideline suggests allowing for up to 40% length variance.
 * 
 * Warnings are logged for keys exceeding 40% variance.
 * Critical violations (>60%) cause the script to exit with code 1.
 */

const fs = require('fs');
const path = require('path');

const I18N_DIR = path.join(__dirname, '../src/assets/i18n');
const EN_FILE = path.join(I18N_DIR, 'en.json');
const UK_FILE = path.join(I18N_DIR, 'uk.json');

const WARNING_THRESHOLD = 0.40; // 40%
const CRITICAL_THRESHOLD = 0.60; // 60%

/**
 * Recursively extracts all key-value pairs from a nested object with dot notation keys.
 */
function flattenObject(obj, prefix = '') {
  const result = {};
  
  for (const key in obj) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    
    if (typeof obj[key] === 'object' && obj[key] !== null && !Array.isArray(obj[key])) {
      Object.assign(result, flattenObject(obj[key], fullKey));
    } else if (typeof obj[key] === 'string') {
      result[fullKey] = obj[key];
    }
  }
  
  return result;
}

console.log('🔍 Validating string length variance...\n');

try {
  const enContent = fs.readFileSync(EN_FILE, 'utf-8');
  const ukContent = fs.readFileSync(UK_FILE, 'utf-8');
  
  const enJson = JSON.parse(enContent);
  const ukJson = JSON.parse(ukContent);
  
  const enFlat = flattenObject(enJson);
  const ukFlat = flattenObject(ukJson);
  
  const warnings = [];
  const critical = [];
  
  for (const key in enFlat) {
    if (!ukFlat[key]) continue;
    
    const enLength = enFlat[key].length;
    const ukLength = ukFlat[key].length;
    
    // Skip if English is very short (1-2 chars like "OK")
    if (enLength <= 2) continue;
    
    // Skip interpolation-only keys
    if (enFlat[key].match(/^\{\{.*\}\}$/)) continue;
    
    const variance = (ukLength - enLength) / enLength;
    
    if (variance > CRITICAL_THRESHOLD) {
      critical.push({
        key,
        enLength,
        ukLength,
        variance: (variance * 100).toFixed(1)
      });
    } else if (variance > WARNING_THRESHOLD) {
      warnings.push({
        key,
        enLength,
        ukLength,
        variance: (variance * 100).toFixed(1)
      });
    }
  }
  
  let hasErrors = false;
  
  if (warnings.length > 0) {
    console.warn(`⚠️  ${warnings.length} warnings (>40% length variance):`);
    warnings.forEach(w => {
      console.warn(`   ${w.key}: ${w.enLength} → ${w.ukLength} chars (+${w.variance}%)`);
    });
    console.warn('   Note: Review UI layouts to ensure no overflow/truncation\n');
  }
  
  if (critical.length > 0) {
    console.error(`❌ ${critical.length} critical violations (>60% length variance):`);
    critical.forEach(c => {
      console.error(`   ${c.key}: ${c.enLength} → ${c.ukLength} chars (+${c.variance}%)`);
    });
    console.error('\n❌ String length validation failed');
    console.error('   Ukrainian translations are excessively longer than English');
    console.error('   Consider using shorter synonyms or adjusting UI layouts');
    hasErrors = true;
  }
  
  if (!hasErrors) {
    if (warnings.length === 0) {
      console.log('✅ All string lengths within acceptable variance');
    } else {
      console.log('✅ No critical length violations (warnings logged above)');
    }
    process.exit(0);
  } else {
    process.exit(1);
  }
  
} catch (error) {
  console.error('❌ Error reading or parsing files:');
  console.error(error.message);
  process.exit(1);
}
