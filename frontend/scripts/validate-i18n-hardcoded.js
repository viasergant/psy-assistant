#!/usr/bin/env node

/**
 * Detects hard-coded strings in HTML templates and TypeScript files.
 * Looks for common patterns:
 * - Text between > and < in HTML (excluding certain tags)
 * - label="...", placeholder="...", prompt="..." attributes
 * - MessageService detail/summary with literal strings in TypeScript
 * 
 * Exits with code 1 if violations are found.
 */

const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const FEATURES_DIR = path.join(__dirname, '../src/app/features');

console.log('🔍 Scanning for hard-coded strings...\n');

/**
 * Run grep to find potential hard-coded strings in templates
 */
function scanFiles() {
  return new Promise((resolve, reject) => {
    // Regex patterns to catch common hard-coded string locations
    const patterns = [
      // HTML: label="Text", placeholder="Text", prompt="Text"
      '(label|placeholder|prompt|weakLabel|mediumLabel|strongLabel|header|summary|detail)="[A-Z][a-z]',
      // HTML: >Text< (but not >{{ or >{)
      '>[A-Z][a-z][a-z]+<',
    ];
    
    const grepPattern = patterns.join('|');
    
    // Grep command to find patterns
    const command = `grep -rn --include=\\*.html --include=\\*.ts -E '${grepPattern}' ${FEATURES_DIR} || true`;
    
    exec(command, (error, stdout, stderr) => {
      if (stderr) {
        reject(new Error(stderr));
      } else {
        resolve(stdout);
      }
    });
  });
}

/**
 * Filter out false positives and known acceptable patterns
 */
function filterResults(output) {
  if (!output) return [];
  
  const lines = output.split('\n').filter(line => line.trim());
  
  return lines.filter(line => {
    // Skip if it contains transloco pipe
    if (line.includes('| transloco')) return false;
    
    // Skip if it contains transloco.translate
    if (line.includes('transloco.translate') || line.includes('transloco.selectTranslate')) return false;
    
    // Skip if it's a comment
    if (line.includes('//') || line.includes('/*') || line.includes('*/')) return false;
    
    // Skip if it's in aria-label with variable
    if (line.includes('aria-label') && line.includes('{{')) return false;
    
    // Skip test files
    if (line.includes('.spec.ts')) return false;
    
    return true;
  });
}

scanFiles()
  .then(output => {
    const violations = filterResults(output);
    
    if (violations.length === 0) {
      console.log('✅ No hard-coded strings found');
      process.exit(0);
    } else {
      console.error(`❌ Found ${violations.length} potential hard-coded strings:\n`);
      violations.forEach(line => console.error(line));
      console.error('\n❌ Hard-coded string validation failed');
      console.error('   All user-visible strings must use Transloco: {{ \'key\' | transloco }}');
      process.exit(1);
    }
  })
  .catch(error => {
    console.error('❌ Error scanning files:');
    console.error(error.message);
    process.exit(1);
  });
