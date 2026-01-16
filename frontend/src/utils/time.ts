import dayjs from 'dayjs';

export function formatLocal(date: Date | string | number | dayjs.Dayjs | null | undefined) {
  if (!date) return '';
  return dayjs(date).format('YYYY-MM-DDTHH:mm:ss');
}

export function formatDisplay(date: string | null | undefined) {
  if (!date) return '';
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss');
}
