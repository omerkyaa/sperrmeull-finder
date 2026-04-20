const {onRequest} = require('firebase-functions/v2/https');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const crypto = require('crypto');

const db = getFirestore();

function mapProductIdToPremiumType(productId = '') {
  const normalized = String(productId).toLowerCase();
  if (normalized.includes('week')) return 'PREMIUM_WEEK';
  if (normalized.includes('month')) return 'PREMIUM_MONTH';
  if (normalized.includes('year') || normalized.includes('annual')) return 'PREMIUM_YEAR';
  return null;
}

function parseEventDate(event, msKey, isoKey) {
  if (event?.[msKey]) {
    const asNumber = Number(event[msKey]);
    if (!Number.isNaN(asNumber) && asNumber > 0) return new Date(asNumber);
  }
  if (event?.[isoKey]) {
    const parsed = new Date(event[isoKey]);
    if (!Number.isNaN(parsed.getTime())) return parsed;
  }
  return null;
}

function addPeriodFromPlan(anchorDate, premiumType) {
  const date = new Date(anchorDate.getTime());
  if (premiumType === 'PREMIUM_WEEK') {
    date.setDate(date.getDate() + 7);
  } else if (premiumType === 'PREMIUM_MONTH') {
    date.setMonth(date.getMonth() + 1);
  } else if (premiumType === 'PREMIUM_YEAR') {
    date.setFullYear(date.getFullYear() + 1);
  }
  return date;
}

function verifySecret(req, secret) {
  if (!secret) return false;

  const authHeader = req.get('authorization') || '';
  const directToken = authHeader.replace(/^Bearer\s+/i, '').trim();
  if (directToken && directToken === secret) {
    return true;
  }

  const signature = req.get('x-revenuecat-signature');
  const rawBody = req.rawBody;
  if (!signature || !rawBody) {
    return false;
  }

  const computed = crypto
    .createHmac('sha256', secret)
    .update(rawBody)
    .digest('hex');

  return crypto.timingSafeEqual(
    Buffer.from(signature, 'utf8'),
    Buffer.from(computed, 'utf8')
  );
}

exports.revenueCatWebhook = onRequest(async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({success: false, error: 'method_not_allowed'});
    return;
  }

  const webhookSecret = process.env.REVENUECAT_WEBHOOK_SECRET;
  if (!webhookSecret) {
    res.status(500).json({success: false, error: 'missing_webhook_secret'});
    return;
  }

  if (!verifySecret(req, webhookSecret)) {
    res.status(401).json({success: false, error: 'invalid_signature'});
    return;
  }

  const rawPayload = req.body || {};
  const event = rawPayload.event || rawPayload;
  const eventType = String(event.type || '').toUpperCase();
  const userId = event.app_user_id || event.original_app_user_id;
  const productId = event.product_id || '';

  if (!eventType || !userId) {
    res.status(400).json({success: false, error: 'invalid_payload'});
    return;
  }

  const premiumType = mapProductIdToPremiumType(productId);
  const purchasedAt =
    parseEventDate(event, 'purchased_at_ms', 'purchased_at') ||
    parseEventDate(event, 'original_purchase_date_ms', 'original_purchase_date') ||
    new Date();
  const expirationAt = parseEventDate(event, 'expiration_at_ms', 'expiration_at');

  const activeEvents = new Set(['INITIAL_PURCHASE', 'RENEWAL', 'PRODUCT_CHANGE']);
  const deactivateEvents = new Set(['CANCELLATION', 'EXPIRATION']);

  const userRef = db.collection('users').doc(userId);
  const update = {
    premiumLastEvent: eventType,
    premiumLastProductId: productId,
    premiumWebhookUpdatedAt: FieldValue.serverTimestamp(),
  };

  if (activeEvents.has(eventType)) {
    const premiumUntil = premiumType
      ? addPeriodFromPlan(purchasedAt, premiumType)
      : (expirationAt || null);

    Object.assign(update, {
      ispremium: true,
      premiumType: premiumType || '',
      premiumuntil: premiumUntil,
      willRenew: true,
      premiumPurchaseAnchor: purchasedAt,
    });
  } else if (deactivateEvents.has(eventType)) {
    Object.assign(update, {
      ispremium: false,
      premiumType: '',
      premiumuntil: null,
      willRenew: false,
      premiumCancelledAt: FieldValue.serverTimestamp(),
    });
  } else {
    res.status(200).json({success: true, ignored: true});
    return;
  }

  await userRef.set(update, {merge: true});
  res.status(200).json({success: true});
});
