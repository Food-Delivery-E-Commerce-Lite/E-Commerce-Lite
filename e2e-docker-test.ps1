$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
function Req($method, $url, $body, $token) {
  $h = @{'Content-Type'='application/json'}
  if ($token) { $h['Authorization'] = "Bearer $token" }
  $b = $null
  if ($body) { $b = ($body | ConvertTo-Json -Depth 6) }
  return Invoke-RestMethod -Uri ($base + $url) -Method $method -Headers $h -Body $b
}
$rnd = Get-Random -Max 999999
$mEmail = "m$rnd@ex.com"
$cEmail = "c$rnd@ex.com"
Write-Host "== merchant register $mEmail"
$m = Req POST '/api/v1/auth/register' @{email=$mEmail; password='Test@12345'; fullName='Merchant'; role='MERCHANT'}
$mTok = $m.data.accessToken
Write-Host "merchant id=$($m.data.user.id)"
Write-Host '== create restaurant'
$rest = Req POST '/api/v1/merchant/restaurants' @{name="Quan $rnd"; description='Ngon'; address='123 ABC'} $mTok
$rid = $rest.data.id
Write-Host "restaurant id=$rid status=$($rest.data.status)"
Write-Host '== admin approve'
$admin = Req POST '/api/v1/auth/login' @{email='admin@fooddelivery.com'; password='Admin@123'}
$aTok = $admin.data.accessToken
$ap = Req PATCH "/api/v1/admin/restaurants/$rid/status" @{status='APPROVED'} $aTok
Write-Host "approved status=$($ap.data.status)"
Write-Host '== merchant add menu'
$item = Req POST "/api/v1/merchant/restaurants/$rid/menu-items" @{name='Com ga'; description='Ngon'; price=50000; categoryId=1} $mTok
$iid = $item.data.id
Write-Host "item id=$iid price=$($item.data.price)"
Write-Host '== customer register'
$c = Req POST '/api/v1/auth/register' @{email=$cEmail; password='Test@12345'; fullName='Customer'; role='CUSTOMER'}
$cTok = $c.data.accessToken
$cid = $c.data.user.id
Write-Host "customer id=$cid"
Write-Host '== customer address'
$addr = Req POST '/api/v1/addresses' @{label='Nha'; street='1 XYZ'; district='Q1'; city='HCM'; isDefault=$true} $cTok
$aid = $addr.data.id
Write-Host "address id=$aid"
Write-Host '== place COD order with voucher SALE10'
$ord = Req POST '/api/v1/orders' @{restaurantId=$rid; addressId=$aid; items=@(@{itemId=$iid; quantity=2}); method='COD'; voucherCode='SALE10'} $cTok
Write-Host "order id=$($ord.data.id) subtotal=$($ord.data.subtotal) discount=$($ord.data.discountAmount) total=$($ord.data.totalAmount) pay=$($ord.data.payStatus)"
$oid = $ord.data.id
Write-Host '== merchant confirm'
$s1 = Req PATCH "/api/v1/merchant/orders/$oid/status" @{status='CONFIRMED'} $mTok
Write-Host "status=$($s1.data.status)"
Write-Host '== merchant preparing->delivering->completed'
Req PATCH "/api/v1/merchant/orders/$oid/status" @{status='PREPARING'} $mTok | Out-Null
Req PATCH "/api/v1/merchant/orders/$oid/status" @{status='DELIVERING'} $mTok | Out-Null
$done = Req PATCH "/api/v1/merchant/orders/$oid/status" @{status='COMPLETED'} $mTok
Write-Host "final status=$($done.data.status) pay=$($done.data.payStatus) (COD completed -> PAID expected)"
Write-Host '== customer my orders'
$mine = Req GET '/api/v1/orders' $null $cTok
Write-Host "my orders count=$($mine.data.Count)"
Write-Host '== ONLINE order + callback + cancel refund'
$ord2 = Req POST '/api/v1/orders' @{restaurantId=$rid; addressId=$aid; items=@(@{itemId=$iid; quantity=1}); method='ONLINE'} $cTok
$oid2 = $ord2.data.id
Write-Host "online order id=$oid2 pay=$($ord2.data.payStatus)"
$cb = Req POST '/api/v1/payments/callback' @{orderId=$oid2; paymentRef="REF$rnd"; success=$true} $null
Write-Host "callback status=$($cb.data.status)"
$cb2 = Req POST '/api/v1/payments/callback' @{orderId=$oid2; paymentRef="REF$rnd"; success=$true} $null
Write-Host "callback idempotent status=$($cb2.data.status)"
$cancel = Req POST "/api/v1/orders/$oid2/cancel" @{reason='doi y'} $cTok
Write-Host "cancelled status=$($cancel.data.status) pay=$($cancel.data.payStatus) (ONLINE PAID cancel -> REFUNDED expected)"
Write-Host '== merchant stats + report + csv'
$stats = Req GET "/api/v1/merchant/restaurants/$rid/stats" $null $mTok
Write-Host ("stats total={0} revenue={1}" -f $stats.data.totalOrders, $stats.data.revenue)
$rep = Req GET "/api/v1/merchant/restaurants/$rid/report?from=2026-10-01&to=2026-11-09" $null $mTok
Write-Host ("merchant report revenue={0} orders={1}" -f $rep.data.revenue, $rep.data.totalOrders)
$csvCode = curl.exe -s -o NUL -w '%{http_code}' -H ('Authorization: Bearer ' + $mTok) ($base + "/api/v1/merchant/restaurants/$rid/report.csv?from=2026-10-01&to=2026-11-09")
Write-Host ("csv={0}" -f $csvCode)
Write-Host '== admin overview'
$ov = Req GET '/api/v1/admin/overview' $null $aTok
Write-Host ("overview users={0} orders={1}" -f $ov.data.totalUsers, $ov.data.totalOrders)
$ar = Req GET '/api/v1/admin/report?from=2026-10-01&to=2026-11-09' $null $aTok
Write-Host ("admin report revenue={0} orders={1}" -f $ar.data.revenue, $ar.data.totalOrders)
Write-Host 'E2E PASS'
