package dev.meetree.shop.ext.http4s

import org.http4s.QueryParamDecoder

import dev.meetree.shop.ext.derevo.Derive

object queryParam extends Derive[QueryParamDecoder]
