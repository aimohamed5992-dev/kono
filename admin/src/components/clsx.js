export function clsx(...args) {
  return args
    .flat(Infinity)
    .filter((x) => typeof x === 'string' && x.length)
    .join(' ')
}
