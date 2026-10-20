"""Recover complete ZIP local records if Windows aapt crashes before the central directory.

Every record's length and CRC must validate; incomplete payloads are rejected.
"""
import sys, struct, zipfile, zlib, io
from pathlib import Path

def repair(path):
    path = Path(path)
    if zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as archive:
            if archive.testzip() is not None:
                raise ValueError('Archive CRC verification failed')
        return
    data = path.read_bytes()
    pos = 0
    entries = []
    while pos < len(data):
        if data[pos:pos+4] != b'PK\x03\x04':
            raise ValueError(f'Unexpected ZIP record at {pos}')
        _, version, flags, method, tm, dt, crc, csize, size, nlen, xlen = struct.unpack_from('<I5H3I2H', data, pos)
        if flags & 9:
            raise ValueError('Encrypted or streaming entries are unsupported')
        name = data[pos+30:pos+30+nlen].decode('utf-8')
        start = pos+30+nlen+xlen
        payload = data[start:start+csize]
        if len(payload) != csize:
            raise ValueError('Truncated entry')
        raw = payload if method == 0 else zlib.decompress(payload, -15) if method == 8 else None
        if raw is None or len(raw) != size or zlib.crc32(raw) & 0xffffffff != crc:
            raise ValueError('Invalid entry: '+name)
        entries.append((name, raw))
        pos = start+csize
    if not entries:
        raise ValueError('No complete entries')
    result = io.BytesIO()
    with zipfile.ZipFile(result, 'w', zipfile.ZIP_STORED) as archive:
        for name, raw in entries:
            archive.writestr(name, raw)
    path.write_bytes(result.getvalue())
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError('Repaired archive failed CRC validation')
    print(f'Recovered {len(entries)} CRC-verified records: {path}')

if __name__ == '__main__':
    if len(sys.argv) == 4 and sys.argv[1] == '--align':
        with zipfile.ZipFile(sys.argv[2]) as source, zipfile.ZipFile(sys.argv[3], 'w', zipfile.ZIP_STORED) as target:
            for entry in source.infolist():
                info = zipfile.ZipInfo(entry.filename)
                base = target.fp.tell()+30+len(entry.filename.encode('utf-8'))
                pad = (-base-4) % 4
                info.extra = struct.pack('<HH', 0xffff, pad) + bytes(pad)
                target.writestr(info, source.read(entry))
    else:
        repair(sys.argv[1])
