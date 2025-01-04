const fs = require('fs')
const path = require('path')
const https = require('https')

const EMOJI_VERSION = '16.0'

main()

async function main () {
  const text = await getTestFile(EMOJI_VERSION)

  console.log(`Format text to json...`)
  const collected = text.trim().split('\n').reduce((accu, line) => {
    if (line.startsWith('# group: ')) {
      console.log(`  Processing ${line.substr(2)}...`)
      accu.group = line.substr(9)
    } else if (line.startsWith('# subgroup: ')) {
      accu.subgroup = line.substr(12)
    } else if (line.startsWith('#')) {
    } else {
      if (!line.includes("skin tone")) {
          const meta = parseLine(line)
          if (meta) {
            accu.full.push(meta)
            accu.compact.push(meta.char)
          } else {
            accu.comments = accu.comments.trim() + '\n\n'
          }
       }
    }
    return accu
  }, { comments: '', full: [], compact: [] })

  console.log(`Processed emojis: ${collected.full.length}`)

  console.log('Write file: emoji.json \n')
  await writeFiles(collected)

  console.log(collected.comments)
}

async function getTestFile (ver) {
  const url = `https://unicode.org/Public/emoji/${ver}/emoji-test.txt`

  process.stdout.write(`Fetch emoji-test.txt (v${EMOJI_VERSION})`)
  return new Promise((resolve, reject) => {
    https.get(url, res => {
      let text = ''
      res.setEncoding('utf8')
      res.on('data', (chunk) => {
        process.stdout.write('.')
        text += chunk
      })
      res.on('end', () => {
        process.stdout.write('\n')
        resolve(text)
      })
      res.on('error', reject)
    })
  })
}

function parseLine (line) {
  const data = line.trim().split(/\s+[;#] /)

  if (data.length !== 3) {
    return null
  }

  const [ , char, unformattedName ] = data[2].match(/^(\S+) E\d+\.\d+ (.+)$/)
  let name = unformattedName.replaceAll(': ', '_').replaceAll(', ', '_').replaceAll('&', 'and').replaceAll('.', '').replaceAll(/[ ]?-[ ]?/g, '_').replaceAll('"', '').replaceAll(' ', '_').toLowerCase()
  return { char, name }
}

const rel = (...args) => path.resolve(__dirname, ...args)

function writeFiles({ full, compact }) {
  fs.writeFileSync(rel('./src/main/resources/emoji.json'), JSON.stringify(full, null, 2), 'utf8')
}
