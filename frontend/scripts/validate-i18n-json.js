#!/usr/bin/env node

/**
 * Validates that translation JSON files are valid JSON syntax.
 * Exits with code 1 if any files are invalid.
 */

const fs = require('fs');
const path = require('path');

const I18N_DIR = path.join(__dirname, '../src/assets/i18n');
const FILES = ['en.json', 'uk.json'];

let hasErrors = false;

console.log('🔍 Validating i18n JSON files...\n');

FILES.forEach(filename => {
  const filePath = path.join(I18N_DIR, filename);
  
  try {
    const content = fs.readFileSync(filePath, 'utf-8');
    JSON.parse(content);
    console.log(`✅ ${filename}: Valid JSON`);
  } catch (error) {
    console.error(`❌ ${filename}: Invalid JSON`);
    console.error(`   Error: ${error.message}`);
    hasErrors = true;
  }
});

console.log('');

if (hasErrors) {
  console.error('❌ JSON validation failed');
  process.exit(1);
} else {
  console.log('✅ All JSON files are valid');
  process.exit(0);
}
