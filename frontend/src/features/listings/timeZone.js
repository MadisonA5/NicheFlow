function createDateTimeFormatter(timeZone) {
    return new Intl.DateTimeFormat('en-CA', {
        timeZone,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hourCycle: 'h23',
    })
}

function dateTimeParts(value, formatter) {
    const parts = formatter.formatToParts(value)
    return Object.fromEntries(parts.map(({type, value: partValue}) =>
        [type, partValue]))
}

function localDateTimeString(parts) {
    return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`
}

export function localDateTimeToInstant(value, timeZone, afterInstant) {
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
        throw new Error('Choose a valid date and time.')
    }

    const [date, time] = value.split('T')
    const [year, month, day] = date.split('-').map(Number)
    const [hour, minute] = time.split(':').map(Number)
    const wallClock = Date.UTC(year, month - 1, day, hour, minute)
    const candidates = []
    const formatter = createDateTimeFormatter(timeZone)

    for (let offsetMinutes = -14 * 60; offsetMinutes <= 14 * 60;
        offsetMinutes += 15) {
        const candidate = wallClock - offsetMinutes * 60_000
        if (localDateTimeString(dateTimeParts(
            new Date(candidate), formatter,
        )) === value) {
            candidates.push(candidate)
        }
    }
    candidates.sort((first, second) => first - second)

    const after = afterInstant ? Date.parse(afterInstant) : -Infinity
    const candidate = candidates.find((instant) => instant > after)
    if (candidate === undefined) {
        throw new Error(
            'That local time does not exist or is not after the start time.',
        )
    }
    return new Date(candidate)
}

export function formatDateTimeInZone(value, timeZone) {
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
        timeZone,
    }).format(new Date(value))
}

export function getNextLocalHour(timeZone) {
    const parts = dateTimeParts(new Date(), createDateTimeFormatter(timeZone))
    const wallClock = Date.UTC(
        Number(parts.year),
        Number(parts.month) - 1,
        Number(parts.day),
        Number(parts.hour),
        Number(parts.minute),
    )
    const minutesUntilNextHour = 60 - Number(parts.minute)
    const roundedHour = new Date(
        wallClock + minutesUntilNextHour * 60_000,
    )
    return localDateTimeString({
        year: String(roundedHour.getUTCFullYear()),
        month: String(roundedHour.getUTCMonth() + 1).padStart(2, '0'),
        day: String(roundedHour.getUTCDate()).padStart(2, '0'),
        hour: String(roundedHour.getUTCHours()).padStart(2, '0'),
        minute: '00',
    })
}
